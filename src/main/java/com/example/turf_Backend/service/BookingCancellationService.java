package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.response.CancelBookingResponse;
import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.entity.Payment;
import com.example.turf_Backend.entity.Slots;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.enums.*;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.BookingMapper;
import com.example.turf_Backend.repository.BookingRepository;
import com.example.turf_Backend.repository.PaymentRepository;
import com.example.turf_Backend.repository.SlotsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationService {
    private final BookingRepository bookingRepository;
    private final SlotsRepository slotsRepository;
    private final PaymentRepository paymentRepository;
    private final RefundService refundService;
    private final BookingMapper bookingMapper;

    @Transactional
    public CancelBookingResponse cancelByCustomer(String bookingId) {
        User customer=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Customer cancellation requested : bookingId ={} ,customerId={}",bookingId,customer.getId());

        Booking booking=bookingRepository.lockBookingForUpdate(bookingId).orElseThrow(()->new CustomException("Booking not found", HttpStatus.NOT_FOUND));

        if (!booking.getCustomer().getId().equals(customer.getId())){
            throw new CustomException("Unauthorized",HttpStatus.FORBIDDEN);
        }
        if (booking.getStatus()!= BookingStatus.CONFIRMED){
            log.info("Booking already  cancelled : {} ",bookingId);
            return bookingMapper.toCancelResponse(booking,booking.getPayment());
        }

        Payment payment=paymentRepository.lockByBookingId(booking.getId())
                .orElseThrow(()->new CustomException("Payment not found",HttpStatus.BAD_REQUEST));

        if (payment.getStatus()!= PaymentStatus.SUCCESS){
            throw new CustomException("Payment not completed",HttpStatus.BAD_REQUEST);
        }
        if (payment.getRefundStatus()!=null){
            log.info("Refund already initiated : payment={} status={}",payment.getId(),payment.getRefundStatus());
            return bookingMapper.toCancelResponse(booking,payment);
        }

        List<Slots> slots=slotsRepository.lockByIdsForUpdate(booking.getSlotIds());

        LocalDateTime slotStart=slots.stream()
                .map(s->LocalDateTime.of(s.getDate(),s.getStartTime()))
                .min(LocalDateTime::compareTo)
                .orElseThrow(()->new CustomException("No SLots Found for booking",HttpStatus.INTERNAL_SERVER_ERROR));

        LocalDateTime now=LocalDateTime.now();

        if (slotStart.isBefore(now.plusHours(4))){
            throw new CustomException("cancellation not allowed within 4 hours of slot start",HttpStatus.BAD_REQUEST);
        }

        booking.setStatus(BookingStatus.CANCELLED_BY_CUSTOMER);
        bookingRepository.save(booking);
        slots.forEach(s->s.setStatus(SlotStatus.AVAILABLE));
        slotsRepository.saveAll(slots);

        int totalPaid= payment.getAmountPaid();
        int platformFee= payment.getPlatformFeePaid();

        int refundAmount=totalPaid-platformFee;
        if (refundAmount<=0){
            throw new CustomException("Refund amount invalid",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        payment.setRefundAmount(refundAmount);
        payment.setRefundReason(RefundReason.CUSTOMER_CANCELLED_BEFORE_CUTOFF);
        payment.setRefundStatus(RefundStatus.REQUESTED);
        paymentRepository.save(payment);
        log.info("Cancellation processed: booking={} refundAmount={} platformFee={}",bookingId,refundAmount,platformFee);
        return bookingMapper.toCancelResponse(booking,payment);
    }
}
