package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.ExecutionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayoutExecutionResponse(Long executionId,
        Long ownerId,
        String ownerName,
        BigDecimal amount,
        ExecutionStatus status,
        int retryCount,
        LocalDateTime createdAt
        ) {
}
