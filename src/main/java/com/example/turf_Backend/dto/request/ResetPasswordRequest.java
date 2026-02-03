package com.example.turf_Backend.dto.request;

public record ResetPasswordRequest(
        String token,
        String newPassword
) {
}
