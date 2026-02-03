package com.example.turf_Backend.service;

import com.example.turf_Backend.entity.*;
import com.example.turf_Backend.enums.*;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.print.Book;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {
     private final PaymentRepository paymentRepository;
     private final BookingRepository bookingRepository;
     private final SlotsRepository slotsRepository;
     private final BookingLedgerRepository bookingLedgerRepository;
     private final PlatformLedgerRepository platformLedgerRepository;



    @Transactional
    public void markPaymentCaptured(String razorpayOrderId, String razorpayPaymentId, long capturedAtUnix, JSONObject entity) {
        log.info("Webhook processing payment.captured: order={}, payment={}",
                razorpayOrderId, razorpayPaymentId);
        //find payment
        Optional<Payment> opt = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
        if (opt.isEmpty()) {
            log.warn("Payment not found for webhook. order={}", razorpayOrderId);
            return;
        }
        Payment payment = opt.get();

        LocalDateTime captureAt=LocalDateTime.ofInstant(
                Instant.ofEpochSecond(capturedAtUnix), ZoneId.of("Asia/Kolkata")
        );
        payment.setGatewayFee(
                BigDecimal.valueOf(entity.getInt("fee")).movePointLeft(2).intValue()
        );
        payment.setGatewayTax(
                BigDecimal.valueOf(entity.getInt("tax")).movePointLeft(2).intValue()
        );
        // 2. IDEMPOTENCY CHECK - check for success
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if(payment.getPaymentCapturedAt()==null)
            {
                payment.setPaymentCapturedAt(captureAt);
            }
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

        LocalDateTime capturedAt=LocalDateTime.ofInstant(
                Instant.ofEpochSecond(capturedAtUnix), ZoneId.of("Asia/Kolkata")
        );
        Booking booking = payment.getBooking();

        Optional<Booking> lockedBookingOpt=bookingRepository.lockBookingForUpdate(booking.getId());
        if (lockedBookingOpt.isEmpty())
        {
            log.warn("webhook: booking disappeared. id={}",booking.getId());
            return;
        }
        Booking lockedbooking=lockedBookingOpt.get();

        if (lockedbooking.getStatus()!= BookingStatus.PENDING_PAYMENT){
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
            lockedbooking.setStatus(BookingStatus.CANCELLED_BY_ADMIN);
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

            lockedbooking.setStatus(BookingStatus.CANCELLED_BY_ADMIN);
            bookingRepository.save(lockedbooking);
            return; // Don't mark SUCCESS
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setPaymentCapturedAt(capturedAt);

        Booking b=payment.getBooking();
        payment.setAmountPaid(payment.getAmount());
        if (payment.getPlatformFeePaid()==null) payment.setPlatformFeePaid(b.getPlatformFee());
        if (payment.getCommissionPaid()==null) payment.setCommissionPaid(b.getCommissionAmount());
        if (payment.getOwnerAmountPaid()==null) payment.setOwnerAmountPaid(b.getOwnerEarning());
        paymentRepository.save(payment);


        lockedbooking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(lockedbooking);

        log.info("Webhook successfully confirmed payment. booking={}, payment={} captureAt={}",
                lockedbooking.getId(), razorpayPaymentId,capturedAt);

        bookingLedgerRepository.save(BookingLedger.builder()
                        .bookingId(b.getId())
                        .ownerId(null)
                        .type(LedgerType.CREDIT)
                        .reason(BookingLedgerReason.PAYMENT_CAPTURED)
                        .amount(BigDecimal.valueOf(payment.getAmount()))
                        .referenceType(ReferenceType.PAYMENT)
                        .referenceId(razorpayPaymentId)
                        .createdAt(capturedAt)
                .build());

    }
    @Transactional
    public void markPaymentRefunded(String razorpayPaymentId) {

        Payment payment=paymentRepository.findByRazorpayPaymentId(razorpayPaymentId)
                .orElseThrow(()-> new CustomException("Payment not found", HttpStatus.NOT_FOUND));

        if (payment.getRefundStatus()==RefundStatus.PROCESSED){
            log.info("Refund already processed for payment={}",payment.getId());
            return;
        }
        payment.setRefundStatus(RefundStatus.PROCESSED);
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        log.info("Refund processed via webhook payment={} booking={}",payment.getId(),payment.getBooking().getId());

        bookingLedgerRepository.save(BookingLedger.builder()
                        .bookingId(payment.getBooking().getId())
                        .type(LedgerType.DEBIT)
                        .reason(BookingLedgerReason.REFUND)
                        .amount(BigDecimal.valueOf(payment.getRefundAmount()))
                        .referenceType(ReferenceType.REFUND)
                        .referenceId(payment.getRazorpayRefundId())
                        .createdAt(LocalDateTime.now())
                .build());

        boolean wasSettled=payment.getSettlementStatus()==SettlementStatus.SETTLED;
        boolean isFullRefund=payment.getRefundAmount()!=null && payment.getRefundAmount().equals(payment.getAmount());
//Note check ledger entry about debit of commission refund while refund

        if (wasSettled){
            platformLedgerRepository.save(PlatformLedger.builder()
                            .type(LedgerType.DEBIT)
                            .reason(PlatformLedgerReason.REFUND)
                            .amount(BigDecimal.valueOf(payment.getRefundAmount()))
                            .bookingId(payment.getBooking().getId())
                            .referenceType(ReferenceType.REFUND)
                            .referenceId(payment.getRazorpayRefundId())
                            .createdAt(LocalDateTime.now())
                    .build());
            if (isFullRefund){
                platformLedgerRepository.save(PlatformLedger.builder()
                        .type(LedgerType.DEBIT)
                        .reason(PlatformLedgerReason.PLATFORM_FEE_REVERSAL)
                        .amount(BigDecimal.valueOf(payment.getPlatformFeePaid()))
                        .bookingId(payment.getBooking().getId())
                        .referenceType(ReferenceType.REFUND)
                        .referenceId(payment.getRazorpayRefundId())
                        .createdAt(LocalDateTime.now())
                        .build());

            }
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void markPaymentSettled(String rpPaymentId, LocalDateTime settledAt) {
        Optional<Payment> opt=paymentRepository.findByRazorpayPaymentId(rpPaymentId);
        if (opt.isEmpty())
        {
            log.warn("Settlement webhook: payment not found , rpPaymentId={}",rpPaymentId);
            return ;
        }
        Payment payment=opt.get();
        //Ignore if settlement already processed
        if (payment.getSettlementStatus()== SettlementStatus.SETTLED){
            log.info("Settlement already processed , rpPaymentId={}",rpPaymentId);
            return ;
        }
        //Ignore if payment wasn't successfully
        if (payment.getStatus()!=PaymentStatus.SUCCESS)
        {
            log.warn("Settlement received for non success payment,. rpPaymentId={}",rpPaymentId);
            return;
        }
        //mark payment settled

        payment.setSettlementStatus(SettlementStatus.SETTLED);
        payment.setSettledAt(settledAt);
        paymentRepository.save(payment);
        log.info("Payment marked as SETTLED successfully booking={},rpPaymentId={}",payment.getBooking().getId(),rpPaymentId);
        bookingLedgerRepository.save(BookingLedger.builder()
                .bookingId(payment.getBooking().getId())
                .ownerId(null)
                .type(LedgerType.CREDIT)
                .reason(BookingLedgerReason.PAYMENT_SETTLED)
                .amount(BigDecimal.valueOf(payment.getAmount()))
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(rpPaymentId)
                .createdAt(settledAt)
                .build());
        platformLedgerRepository.save(PlatformLedger.builder()
                        .type(LedgerType.CREDIT)
                        .reason(PlatformLedgerReason.SETTLEMENT)
                        .amount(BigDecimal.valueOf(payment.getAmount()))
                        .bookingId(payment.getBooking().getId())
                        .ownerId(payment.getBooking().getTurf().getOwner().getId())
                        .referenceType(ReferenceType.PAYMENT)
                        .referenceId(rpPaymentId)
                        .createdAt(settledAt)
                .build());
        platformLedgerRepository.save(PlatformLedger.builder()
                .type(LedgerType.CREDIT)
                .reason(PlatformLedgerReason.PLATFORM_FEE)
                .amount(BigDecimal.valueOf(payment.getPlatformFeePaid()))
                .bookingId(payment.getBooking().getId())
                .ownerId(payment.getBooking().getTurf().getOwner().getId())
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(rpPaymentId)
                .createdAt(settledAt)
                .build());

        platformLedgerRepository.save(PlatformLedger.builder()
                .type(LedgerType.CREDIT)
                .reason(PlatformLedgerReason.COMMISSION)
                .amount(BigDecimal.valueOf(payment.getCommissionPaid()))
                .bookingId(payment.getBooking().getId())
                .ownerId(payment.getBooking().getTurf().getOwner().getId())
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(rpPaymentId)
                .createdAt(settledAt)
                .build());

        platformLedgerRepository.save(PlatformLedger.builder()
                .type(LedgerType.DEBIT)
                .reason(PlatformLedgerReason.GATEWAY_FEE)
                .amount(BigDecimal.valueOf(payment.getGatewayFee()))
                .bookingId(payment.getBooking().getId())
                .ownerId(payment.getBooking().getTurf().getOwner().getId())
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(rpPaymentId)
                .createdAt(settledAt)
                .build());

        platformLedgerRepository.save(PlatformLedger.builder()
                .type(LedgerType.DEBIT)
                .reason(PlatformLedgerReason.GATEWAY_GST)
                .amount(BigDecimal.valueOf(payment.getGatewayTax()))
                .bookingId(payment.getBooking().getId())
                .ownerId(payment.getBooking().getTurf().getOwner().getId())
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(rpPaymentId)
                .createdAt(settledAt)
                .build());

        bookingLedgerRepository.save(BookingLedger.builder()
                .bookingId(payment.getBooking().getId())
                .ownerId(null)
                .type(LedgerType.DEBIT)
                .reason(BookingLedgerReason.PLATFORM_FEE)
                .amount(BigDecimal.valueOf(payment.getPlatformFeePaid()))
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(rpPaymentId)
                .createdAt(settledAt)
                .build());

        bookingLedgerRepository.save(BookingLedger.builder()
                .bookingId(payment.getBooking().getId())
                .ownerId(null)
                .type(LedgerType.DEBIT)
                .reason(BookingLedgerReason.COMMISSION)
                .amount(BigDecimal.valueOf(payment.getCommissionPaid()))
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(rpPaymentId)
                .createdAt(settledAt)
                .build());
    }

    //Helper Method
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
