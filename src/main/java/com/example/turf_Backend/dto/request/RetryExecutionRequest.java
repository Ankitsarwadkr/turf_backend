package com.example.turf_Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetryExecutionRequest {
    @NotBlank
    private String note;

}
