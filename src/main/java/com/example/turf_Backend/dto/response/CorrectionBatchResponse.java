package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.BatchStatus;

import java.math.BigDecimal;

public record CorrectionBatchResponse(
        Long batchId,
        Long originalExecutionId,
        BigDecimal correctedAmount,
        BatchStatus status,
        String reason
) {
}
