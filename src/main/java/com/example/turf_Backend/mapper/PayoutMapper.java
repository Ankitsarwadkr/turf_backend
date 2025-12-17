package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.response.MarkExecutionFailedResponse;
import com.example.turf_Backend.dto.response.PayoutBatchResponse;
import com.example.turf_Backend.entity.PayoutBatch;
import com.example.turf_Backend.entity.PayoutExecution;
import org.springframework.stereotype.Component;

@Component
public class PayoutMapper {
    public PayoutBatchResponse toResponse(PayoutBatch batch)
    {
        return PayoutBatchResponse.builder()
                .batchId(batch.getId())
                .weekStart(batch.getWeekStart())
                .weekEnd(batch.getWeekEnd())
                .totalAmount(batch.getTotalAmount())
                .totalOwners(batch.getTotalOwners())
                .status(batch.getStatus())
                .createdAt(batch.getCreatedAt())
                .build();
    }

    public MarkExecutionFailedResponse buildFailedResponse(PayoutExecution ex)
    {   return MarkExecutionFailedResponse.builder()
            .executionId(ex.getId())
            .status(ex.getStatus())
            .failureCode(ex.getFailureCode())
            .failureReason(ex.getFailureReason())
            .failedBy(ex.getFailedBy())
            .failedAt(ex.getFailedAt())
            .build();
    }
}
