package com.example.turf_Backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerLedgerRow(
        LocalDateTime date,
        String bookingId,
        String type,
        String reason,
        BigDecimal amount,
        String reference
) {
}
