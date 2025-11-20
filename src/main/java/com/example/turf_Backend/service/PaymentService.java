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
import java.security.SignatureException;
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
        if (booking.getExpireAt()!=null && booking.getExpireAt().isBefore(LocalDateTime.now()))
        {
            throw new CustomException("Booking expired ",HttpStatus.BAD_REQUEST);
        }
        Payment payment=paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(()->new CustomException("Payment Record not found ",HttpStatus.NOT_FOUND));

        boolean signatureValid=verifySignature(request);
        //Double lock slots

        List<Slots> lockedSlots=slotsRepository.lockByIdsForUpdate(booking.getSlotId());

        if (!signatureValid)
        {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            paymentRepository.save(payment);
            //release slots (if any are Booked)
            if (!lockedSlots.isEmpty())
            {
                lockedSlots.forEach(s->{
                    if (s.getStatus()!= SlotStatus.AVAILABLE)
                        s.setStatus(SlotStatus.AVAILABLE);
                });
                slotsRepository.saveAll(lockedSlots);
            }
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            emailService.sendPaymentFailed(booking.getCustomer().getEmail(),booking);
            log.warn("Payment signature invalid Booking cancelled and slots released. booking={},rpOrder={}",booking.getId(),request.getRazorpayOrderId());
            return mapper.toVerifyResponse(payment,false);
        }

        // --- robust validation after double-lock ---
        List<Long> bookingSlotIds=booking.getSlotId()==null? List.of():
                booking.getSlotId();
        List<Slots> locledSlots =slotsRepository.lockByIdsForUpdate(bookingSlotIds);
        List<Long> expectedSlotIds = bookingSlotIds;

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
        Optional<Payment> opt=paymentRepository.findByRazorpayOrderId(razorpayOrderId);
        if (opt.isEmpty())return;
        Payment p=opt.get();

        p.setRazorpayPaymentId(razorpayPaymentId);
        p.setStatus(PaymentStatus.SUCCESS);
        p.setPaymentTime(LocalDateTime.now());
        paymentRepository.save(p);

        Booking b=p.getBooking();
        if (b.getStatus()!=BookingStatus.CONFIRMED)
        {
            b.setStatus(BookingStatus.CONFIRMED);
        }
        bookingRepository.save(b);
    log.info("Webhook captured payment :rpOder={},rpPaymentId={}, booking={}",razorpayOrderId,razorpayPaymentId,p.getBooking().getId());
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
}
