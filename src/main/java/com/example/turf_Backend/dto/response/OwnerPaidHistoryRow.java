package com.example.turf_Backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerPaidHistoryRow(
        BigDecimal amount,
        LocalDateTime paidAt,
        String reference
) {
}
