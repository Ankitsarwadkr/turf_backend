package com.example.turf_Backend.dto.request;

import com.example.turf_Backend.enums.ExecutionFailureReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkExecutionFailedRequest {
    @NotNull
    private ExecutionFailureReason failureCode;

    @NotBlank
    @Size(max = 500)
    private String failureReason;
}
