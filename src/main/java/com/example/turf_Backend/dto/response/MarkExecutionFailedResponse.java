package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.ExecutionFailureReason;
import com.example.turf_Backend.enums.ExecutionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MarkExecutionFailedResponse {
    private Long executionId;
    private ExecutionStatus status;
    private ExecutionFailureReason failureCode;
    private String failureReason;

    private Long failedBy;
    private LocalDateTime failedAt;
}
