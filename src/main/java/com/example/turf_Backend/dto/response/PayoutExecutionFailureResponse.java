package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.ExecutionFailureReason;

import java.time.LocalDateTime;

public record PayoutExecutionFailureResponse(
        int attemptNumber,
        ExecutionFailureReason failureCode,
        String failureReason,
        LocalDateTime failedAt,
        Long failedBy
) {
}
