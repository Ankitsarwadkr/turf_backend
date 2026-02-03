package com.example.turf_Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileRequest(
        @NotBlank String name,
        @Pattern(regexp = "^[0-9]{10}$") String mobileNo
) {
}
