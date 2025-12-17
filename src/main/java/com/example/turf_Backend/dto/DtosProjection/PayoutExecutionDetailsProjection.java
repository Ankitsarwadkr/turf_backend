package com.example.turf_Backend.dto.DtosProjection;

import com.example.turf_Backend.enums.ExecutionFailureReason;
import com.example.turf_Backend.enums.ExecutionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayoutExecutionDetailsProjection(
        Long executionId,
        Long batchId,
        Long ownerId,
        String ownerName,
        BigDecimal amount,
        ExecutionStatus status,
        ExecutionFailureReason failureCode,
        String failureReason,
        LocalDateTime failedAt,
        Long failedBy,

        int retryCount,
        LocalDateTime lastRetryAt,
        LocalDateTime paidAt,
        Long paidBy,
        String paymentReference,
        LocalDateTime createdAt
) {
}
