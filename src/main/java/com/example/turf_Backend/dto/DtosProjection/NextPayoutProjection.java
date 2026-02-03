package com.example.turf_Backend.dto.DtosProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface NextPayoutProjection {
    String getBookingId();
    BigDecimal getAmount();
    LocalDateTime getCreatedAt();
    String getReason();
}
