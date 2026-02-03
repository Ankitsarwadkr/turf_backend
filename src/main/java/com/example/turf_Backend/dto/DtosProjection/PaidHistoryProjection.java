package com.example.turf_Backend.dto.DtosProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaidHistoryProjection {
    BigDecimal getAmount();
    LocalDateTime getCreatedAt();
    String getReferenceId();
}
