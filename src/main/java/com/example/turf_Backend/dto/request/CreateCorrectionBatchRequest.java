package com.example.turf_Backend.dto.request;

import java.math.BigDecimal;

public record CreateCorrectionBatchRequest(
        Long executionId,
        BigDecimal correctedAmount,
        String reason
) {
}
