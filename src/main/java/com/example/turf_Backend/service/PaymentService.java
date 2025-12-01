package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.VerifyPaymentRequest;
import com.example.turf_Backend.dto.response.PaymentOderResponse;
import com.example.turf_Backend.dto.response.VerifyPaymentResponse;
import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.entity.Payment;
import com.example.turf_Backend.entity.Slots;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.enums.BookingStatus;
import com.example.turf_Backend.enums.PaymentStatus;
import com.example.turf_Backend.enums.SlotStatus;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.PaymentMapper;
import com.example.turf_Backend.repository.BookingRepository;
import com.example.turf_Backend.repository.PaymentRepository;
import com.example.turf_Backend.repository.SlotsRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.awt.print.Book;
import java.security.SignatureException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final SlotsRepository slotsRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper mapper;
    private final EmailService emailService;



    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;
    private static final String CURRENCY="INR";

    public PaymentOderResponse createOrder(String bookingId) {

        User customer=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long customerId=customer.getId();

        Booking booking=bookingRepository.findById(bookingId)
                .orElseThrow(()->new CustomException("Booking not found ", HttpStatus.NOT_FOUND));

        if (!booking.getCustomer().getId().equals(customerId))
        {
            throw new CustomException("Unauthorized booking access",HttpStatus.FORBIDDEN);
        }
        if (booking.getStatus()==BookingStatus.CONFIRMED)
        {
            throw new CustomException("Booking already confirmed",HttpStatus.BAD_REQUEST);
        }
        if (booking.getExpireAt()!=null && booking.getExpireAt().isBefore(LocalDateTime.now()))
        {
            throw new CustomException("Booking expired",HttpStatus.BAD_REQUEST);
        }
        Optional<Payment>existingPending =paymentRepository.findFirstByBookingIdAndStatus(bookingId,PaymentStatus.PENDING);
        if (existingPending.isPresent())
        {
            log.info("Reusing existing pending payment for booking={}",bookingId);
            return mapper.toOrderResponse(existingPending.get());
        }
        Optional<Payment> existingAny=paymentRepository.findByBookingId(bookingId);
        if (existingAny.isPresent())
        {
            Payment p=existingAny.get();
            if (p.getStatus()==PaymentStatus.SUCCESS)
            {
                log.info("Payment already successfull for booking={} returing existing payment" ,bookingId);
                return mapper.toOrderResponse(p);
            }
        }

        try
        {
            RazorpayClient client=new RazorpayClient(key,secret);
            JSONObject request=new JSONObject();
            request.put("amount",booking.getAmount()*100);
            request.put("currency",CURRENCY);
            request.put("receipt",bookingId);

            Order order=client.orders.create(request);
            String rpOrderId=order.get("id");

            Payment  payment=Payment.newPending(booking,rpOrderId, booking.getAmount(), CURRENCY);
            paymentRepository.save(payment);
            log.info("Created Razorpay order : booking={},rpOrderId={},amount={}",bookingId,rpOrderId,booking.getAmount());

            return mapper.toOrderResponse(payment);
        }
        catch (Exception e)
        {
            log.error("Failed to create razorpay order for booking {}:{}",bookingId,e.getMessage(),e);
            throw  new CustomException("Failed to create Razorpay order: "+ e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        User customer=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long customerId=customer.getId();

        Booking booking=bookingRepository.findById(request.getBookingId())
                .orElseThrow(()->new CustomException("Booking not found ", HttpStatus.NOT_FOUND));

        if (!booking.getCustomer().getId().equals(customerId))
        {
            throw new CustomException("Unauthorized booking access",HttpStatus.FORBIDDEN);
        }
        //prevent duplicate /late verification
        if (booking.getStatus()==BookingStatus.CONFIRMED)
        {
            throw new CustomException("Booking already confiremed ",HttpStatus.BAD_REQUEST);
        }
        Payment payment=paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(()->new CustomException("Payment Record not found ",HttpStatus.NOT_FOUND));

        boolean signatureValid=verifySignature(request);
        List<Slots> lockedSlots=slotsRepository.lockByIdsForUpdate(booking.getSlotIds());
        LocalDateTime now=LocalDateTime.now();
        boolean isExpired=booking.getExpireAt()!=null && booking.getExpireAt().isBefore(now);

        if (isExpired){
            long minutesLate=java.time.Duration.between(booking.getExpireAt(),now).toMinutes();
            log.warn("Payment verification attempted after expiry. booking ={},expireAt={},now={},minutesLate={}",booking.getId(),booking.getExpireAt(),now,minutesLate);
            //Hybrid Logic whether to accept or reject
            //check 1: Is signature Valid?
            if (!signatureValid)
            {
                //if signature is not valid just reject the booking
                payment.setStatus(PaymentStatus.FAILED);
                payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
                paymentRepository.save(payment);

                releaseSlots(lockedSlots);
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);

                throw new CustomException("Booking expired and payment verification failed",HttpStatus.BAD_REQUEST);
            }
            //check 2 With in grace period ?
            if (minutesLate<=2)
            {
                log.info("within grace period.Accepting late payment. minutesLate={}",minutesLate);

            } else if (lockedSlots.stream().allMatch(s->s.getStatus()==SlotStatus.AVAILABLE)) {
                log.info("Slots still available . Accepting late payment. minutesLate={}",minutesLate);
                lockedSlots.forEach(s->s.setStatus(SlotStatus.BOOKED));
                slotsRepository.saveAll(lockedSlots);
            }
            else {
                log.warn("Rejecting late payment-slots no longer available. booking={}",booking.getId());

                payment.setStatus(PaymentStatus.FAILED);
                payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
                paymentRepository.save(payment);

                releaseSlots(lockedSlots);
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);
                emailService.sendPaymentFailed(booking.getCustomer().getEmail(), booking);

                log.error("User paid but booking expired and slot taken . need refund. paymentId={}",request.getRazorpayPaymentId());

                throw new CustomException("Booking expired and slot are no longer available ."+" Refund will be processed within 5-7 business days. "+HttpStatus.BAD_REQUEST);
            }
        }

        // --- robust validation after double-lock ---

        List<Long> expectedSlotIds = booking.getSlotIds()==null? List.of():
                booking.getSlotIds();

// 1) Ensure we locked all requested slots
        if (lockedSlots.size() != expectedSlotIds.size()) {
            // Mark payment failed, cancel booking, release any locked slot that is BOOKED
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            paymentRepository.save(payment);

            List<Slots> toRelease = lockedSlots.stream()
                    .filter(s -> s.getStatus() == SlotStatus.BOOKED)
                    .toList();
            if (!toRelease.isEmpty()) {
                toRelease.forEach(s -> s.setStatus(SlotStatus.AVAILABLE));
                slotsRepository.saveAll(toRelease);
            }

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            emailService.sendPaymentFailed(booking.getCustomer().getEmail(),booking);
            log.warn("Verification failed — slot count mismatch. locked={} expected={} booking={} rpOrder={}",
                    lockedSlots.size(), expectedSlotIds.size(), booking.getId(), request.getRazorpayOrderId());

            return mapper.toVerifyResponse(payment, false);
        }

// 2) Collect problematic slots (not BOOKED)
        List<Slots> problematic = lockedSlots.stream()
                .filter(s -> s.getStatus() != SlotStatus.BOOKED)
                .toList();

        if (!problematic.isEmpty()) {
            // Build diagnostic
            String details = problematic.stream()
                    .map(s -> s.getId() + ":" + s.getStatus())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            // Mark payment failed
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            paymentRepository.save(payment);

            // Release only slots currently BOOKED (don't override UNAVAILABLE)
            List<Slots> toRelease = lockedSlots.stream()
                    .filter(s -> s.getStatus() == SlotStatus.BOOKED)
                    .toList();
            if (!toRelease.isEmpty()) {
                toRelease.forEach(s -> s.setStatus(SlotStatus.AVAILABLE));
                slotsRepository.saveAll(toRelease);
            }

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            emailService.sendPaymentFailed(booking.getCustomer().getEmail(),booking);
            log.warn("Verification failed — slots changed before verify. booking={} rpOrder={} details={}",
                    booking.getId(), request.getRazorpayOrderId(), details);

            return mapper.toVerifyResponse(payment, false);
        }

        // If we reach here → all locked slots are BOOKED and count matches → continue

        // if signature valid update payment and booking
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentTime(LocalDateTime.now());
        paymentRepository.save(payment);
        //update booking
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        emailService.sendBookingConfirmed(booking.getCustomer().getEmail(),booking);
        log.info("payment verified and booking confirmed , booking ={},rpPaymentId={}",booking.getId(),request.getRazorpayPaymentId());

        return mapper.toVerifyResponse(payment,true);
    }

    // Verify razorpay signature loacally (HMAC SHA256)

    private boolean verifySignature(VerifyPaymentRequest request)
    {
        try
        {
            String payload=request.getRazorpayOrderId()+ "|"+ request.getRazorpayPaymentId();
            String actual=hmacSha256(payload,secret);
            return actual.equals(request.getRazorpaySignature());
        }
        catch (Exception e)
        {
            log.error("Signature verification error ",e);
            return  false;
        }
    }
    private String hmacSha256(String data,String key) throws
            SignatureException{
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes());

            StringBuilder hex = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        }
        catch (Exception e)
        {
            throw new SignatureException("Error creating HMACSHA256 signature");
        }
    }
    @Transactional
    public void markPaymentCaptured(String razorpayOrderId, String razorpayPaymentId) {
        log.info("Webhook processing payment.captured: order={}, payment={}",
                razorpayOrderId, razorpayPaymentId);
        //find payment
        Optional<Payment> opt = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
        if (opt.isEmpty()) {
            log.warn("Payment not found for webhook. order={}", razorpayOrderId);
            return;
        }
        Payment payment = opt.get();

        // 2. IDEMPOTENCY CHECK - check for success
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already SUCCESS (likely from verifyPayment). " +
                    "Webhook arriving late, skipping. order={}", razorpayOrderId);
            return;
        }
        //check for failed
        if (payment.getStatus()==PaymentStatus.FAILED)
        {
            log.info("Payment already failed. Skipping duplicate order={}",razorpayOrderId);
            return;
        }
        Booking booking = payment.getBooking();

        Optional<Booking> lockedBookingOpt=bookingRepository.lockBookingForUpdate(booking.getId());
        if (lockedBookingOpt.isEmpty())
        {
            log.warn("webhook: booking disappeared. id={}",booking.getId());
            return;
        }
        Booking lockedbooking=lockedBookingOpt.get();

        if (lockedbooking.getStatus()!=BookingStatus.PENDING_PAYMENT){
            log.info("webhook : booking status changes to {},While for lock . Skipping. id={}",lockedbooking.getStatus(),lockedbooking.getId());
            return;
        }

        List<Slots> lockedSlots = slotsRepository.lockByIdsForUpdate(lockedbooking.getSlotIds());


        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = lockedbooking.getExpireAt() != null &&
                lockedbooking.getExpireAt().isBefore(now);

        if (isExpired) {
            long minutesLate = java.time.Duration.between(lockedbooking.getExpireAt(), now).toMinutes();

            log.warn("Webhook received after expiry. booking={}, expireAt={}, now={}, minutesLate={}",
                    lockedbooking.getId(), lockedbooking.getExpireAt(), now, minutesLate);

            // Within grace period?
            if (minutesLate <= 2) {
                log.info("Webhook: within grace period. Accepting. minutesLate={}", minutesLate);
                // Continue to SUCCESS below
            }
            // Slots still available?
            else if (lockedSlots.stream().allMatch(s -> s.getStatus() == SlotStatus.AVAILABLE)) {
                log.info("Webhook: slots still available. Accepting late payment. minutesLate={}",
                        minutesLate);
                // Re-book slots
                lockedSlots.forEach(s -> s.setStatus(SlotStatus.BOOKED));
                slotsRepository.saveAll(lockedSlots);
            }
            else {
                log.warn("Webhook: rejecting late payment - slots no longer available. booking={}",
                        lockedbooking.getId());

                payment.setStatus(PaymentStatus.FAILED);
                payment.setRazorpayPaymentId(razorpayPaymentId);
                paymentRepository.save(payment);

                releaseSlots(lockedSlots);
                lockedbooking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(lockedbooking);
                // TODO: Initiate refund
                log.error("Webhook: user paid but slot taken. Need refund. paymentId={}", razorpayPaymentId);
                return;
            }
        }
        List<Long> expectedSlotIds = lockedbooking.getSlotIds()== null ? List.of() : lockedbooking.getSlotIds();

        if (lockedSlots.size() != expectedSlotIds.size()) {
            log.warn("Webhook: slot count mismatch. locked={} expected={} booking={}",
                    lockedSlots.size(), expectedSlotIds.size(), lockedbooking.getId());

            payment.setStatus(PaymentStatus.FAILED);
            payment.setRazorpayPaymentId(razorpayPaymentId);
            paymentRepository.save(payment);

            List<Slots> toRelease = lockedSlots.stream()
                    .filter(s -> s.getStatus() == SlotStatus.BOOKED)
                    .toList();
            if (!toRelease.isEmpty()) {
                toRelease.forEach(s -> s.setStatus(SlotStatus.AVAILABLE));
                slotsRepository.saveAll(toRelease);
            }
            lockedbooking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(lockedbooking);
            return;
        }

        List<Slots> problematic = lockedSlots.stream()
                .filter(s -> s.getStatus() != SlotStatus.BOOKED)
                .toList();
        if (!problematic.isEmpty()) {
            String details = problematic.stream()
                    .map(s -> s.getId() + ":" + s.getStatus())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            log.warn("Webhook: slots changed before processing. booking={} details={}",
                    lockedbooking.getId(), details);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setRazorpayPaymentId(razorpayPaymentId);
            paymentRepository.save(payment);

            List<Slots> toRelease = lockedSlots.stream()
                    .filter(s -> s.getStatus() == SlotStatus.BOOKED)
                    .toList();
            if (!toRelease.isEmpty()) {
                toRelease.forEach(s -> s.setStatus(SlotStatus.AVAILABLE));
                slotsRepository.saveAll(toRelease);
            }

            lockedbooking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(lockedbooking);
            return; // Don't mark SUCCESS
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentTime(LocalDateTime.now());
        paymentRepository.save(payment);

        lockedbooking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(lockedbooking);

        log.info("Webhook successfully confirmed payment. booking={}, payment={}",
               lockedbooking.getId(), razorpayPaymentId);
    }
    @Transactional
    public void markPaymentRefunded(String razorpayPatmentId) {
        Optional<Payment> opt=paymentRepository.findAll()
                .stream()
                .filter(pay->razorpayPatmentId.equals(pay.getRazorpayPaymentId()))
                .findFirst();
        if (opt.isEmpty())return;
        Payment p=opt.get();
        p.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(p);

        Booking b=p.getBooking();
        b.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(b);
        log.info("webhook refunded payment : rpPaymentId={},booking={}",razorpayPatmentId,p.getBooking().getId());
    }
    private void releaseSlots(List<Slots> slots)
    {
        if (!slots.isEmpty())
        {
            slots.forEach(s->{
                if (s.getStatus()==SlotStatus.BOOKED)
                {
                    s.setStatus(SlotStatus.AVAILABLE);
                }
            });
            slotsRepository.saveAll(slots);
        }
    }
}
