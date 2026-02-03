package com.example.turf_Backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerWeeklyLedgerResponse(
        String statementId,
        String ownerName,
        Long ownerId,
        LocalDate weekStart,
        LocalDate weekEnd,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        List<OwnerLedgerRow> rows
) {
}
