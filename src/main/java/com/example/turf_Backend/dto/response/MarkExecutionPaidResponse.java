package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.ExecutionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MarkExecutionPaidResponse {
    private Long executionId;
    private ExecutionStatus status;

    private Long paidBy;
    private LocalDateTime paidAt;
    private String paymentReference;
}
