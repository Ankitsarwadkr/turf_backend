package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.BatchStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BatchProcessingResponse {
    private Long batchId;
    private BatchStatus status;
    private int executionCount;
}
