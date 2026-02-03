package com.example.turf_Backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerNextPayoutRow(
        String bookingId ,
        BigDecimal amount,
        LocalDateTime earnedAt,
        String reason) {
}
