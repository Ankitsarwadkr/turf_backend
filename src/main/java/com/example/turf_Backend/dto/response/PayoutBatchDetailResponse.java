package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.BatchStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PayoutBatchDetailResponse {
    private Long batchId;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private BigDecimal totalAmount;
    private Integer totalOwners;
    private BatchStatus status;
    private LocalDateTime createdAt;

    private List<OwnerPayoutGroup> owners;
}
