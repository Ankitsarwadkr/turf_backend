package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.ExecutionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RetryExecutionResponse {
    private Long executionId;
    private ExecutionStatus status;
    private int retryCount;
    private LocalDateTime lastRetryAt;
}
