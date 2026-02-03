package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.response.*;
import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.entity.BookingLedger;
import com.example.turf_Backend.enums.LedgerType;
import com.example.turf_Backend.repository.BookingLedgerRepository;
import com.example.turf_Backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerLedgerService {
    private final UserRepository userRepository;

    private final BookingLedgerRepository repository;

    public OwnerBalanceResponse getBalance(Long ownerId) {
        BigDecimal balance = repository.getOwnerBalance(ownerId);

        log.info("LEDGER_BALANCE owner={} balance={}", ownerId, balance);

        return new OwnerBalanceResponse(balance);
    }

    public List<OwnerNextPayoutRow> getNextPayout(Long ownerId) {

        List<OwnerNextPayoutRow> rows =
                repository.findPendingOwnerEarnings(ownerId)
                        .stream()
                        .map(p -> new OwnerNextPayoutRow(
                                p.getBookingId(),
                                p.getAmount(),
                                p.getCreatedAt(),
                                p.getReason()
                        ))
                        .toList();

        BigDecimal total = rows.stream()
                .map(OwnerNextPayoutRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("LEDGER_PENDING owner={} rows={} total={}",
                ownerId, rows.size(), total);

        return rows;
    }

    public List<OwnerPaidHistoryRow> getPaidHistory(Long ownerId) {

        List<OwnerPaidHistoryRow> rows =
                repository.findOwnerHistory(ownerId)
                        .stream()
                        .map(p -> new OwnerPaidHistoryRow(
                                p.getAmount(),
                                p.getCreatedAt(),
                                p.getReferenceId()
                        ))
                        .toList();

        BigDecimal totalPaid = rows.stream()
                .map(OwnerPaidHistoryRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("LEDGER_HISTORY owner={} rows={} totalPaid={}",
                ownerId, rows.size(), totalPaid);

        return rows;
    }

    @Transactional(readOnly = true)
    public OwnerWeeklyLedgerResponse getWeeklyLedger(Long ownerId, LocalDate start, LocalDate end) {

        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.atTime(23,59,59);

        BigDecimal opening=repository.getOpeningBalance(ownerId,s);
        List<BookingLedger> rows=repository.findOwnerLedgerRange(ownerId,s,e);

        BigDecimal running=opening;
        List<OwnerLedgerRow> mapped=new ArrayList<>();
        for (BookingLedger l :rows){
            BigDecimal signed=l.getType()== LedgerType.CREDIT ? l.getAmount()
                    : l.getAmount().negate();
            running=running.add(signed);
            mapped.add(new OwnerLedgerRow(
                    l.getCreatedAt(),
                    l.getBookingId(),
                    l.getType().name(),
                    l.getReason().name(),
                    signed,
                    l.getReferenceId()
            ));
        }
        return new OwnerWeeklyLedgerResponse(
                "Ledger-"+ownerId+"-"+start,
                userRepository.getName(ownerId),
                ownerId,
                start,
                end,
                opening,
                running,
                mapped
        );
    }
}