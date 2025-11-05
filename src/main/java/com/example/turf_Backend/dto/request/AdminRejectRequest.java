package com.example.turf_Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminRejectRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
