package com.example.turf_Backend.service;

import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.entity.BookingLedger;
import com.example.turf_Backend.entity.OwnerEarning;
import com.example.turf_Backend.entity.Payment;
import com.example.turf_Backend.enums.BookingLedgerReason;
import com.example.turf_Backend.enums.LedgerType;
import com.example.turf_Backend.enums.ReferenceType;
import com.example.turf_Backend.enums.SettlementStatus;
import com.example.turf_Backend.repository.BookingLedgerRepository;
import com.example.turf_Backend.repository.OwnerEarningRepository;
import com.example.turf_Backend.repository.PayoutExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OwnerEarningsService {
    private final OwnerEarningRepository ownerEarningRepository;
    private final PayoutExecutionRepository payoutExecutionRepository;
    private final BookingLedgerRepository bookingLedgerRepository;

    @Transactional
    public void createFromBooking(Booking booking) {
        if (ownerEarningRepository.existsByBookingId(booking.getId())){
            return;
        }
        LocalDateTime slotEnd=booking.getSlotEndDateTime();
        if (slotEnd==null || slotEnd.isAfter(LocalDateTime.now())){
            return;
        }

        Payment payment=booking.getPayment();

        if ( payment ==null || payment.getSettlementStatus()!= SettlementStatus.SETTLED || payment.getSettledAt()==null){
            return;
        }
        OwnerEarning earning=OwnerEarning.builder()
                .ownerId(booking.getTurf().getOwner().getId())
                .bookingId(booking.getId())
                .amount(BigDecimal.valueOf(booking.getOwnerEarning()))
                .slotEndDateTime(slotEnd)
                .earnedAt(slotEnd)
                .paidOut(false)
                .build();

        ownerEarningRepository.save(earning);
        log.info("Owner Earning created booking = {} owner ={} amount ={}",booking.getId(),earning.getOwnerId(),earning.getAmount());
        bookingLedgerRepository.save(BookingLedger.builder()
                        .bookingId(earning.getBookingId())
                        .ownerId(earning.getOwnerId())
                        .type(LedgerType.CREDIT)
                        .reason(BookingLedgerReason.OWNER_EARNING)
                        .amount(earning.getAmount())
                        .referenceType(ReferenceType.PAYMENT)
                        .referenceId(payment.getId())
                        .createdAt(earning.getEarnedAt())
                .build());

    }


}
