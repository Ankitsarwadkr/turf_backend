package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PayoutBatchResponse {
    private Long batchId;
    private LocalDate weekStart;
    private LocalDate weekEnd;

    private BigDecimal totalAmount;
    private Integer totalOwners;
    private BatchStatus status;
    private LocalDateTime createdAt;

}
