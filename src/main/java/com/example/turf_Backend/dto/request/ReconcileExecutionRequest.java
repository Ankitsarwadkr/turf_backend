package com.example.turf_Backend.dto.request;

import com.example.turf_Backend.enums.ReconciliationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReconcileExecutionRequest {
    @NotNull
    private ReconciliationStatus status;

    @Size(max = 500)
    private String note;
}
