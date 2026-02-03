package com.example.turf_Backend.dto.response;

public record ProfileResponse(
        Long id,
        String role,
        String name,
        String email,
        String mobileNo,
        String subscriptionStatus,
        Double subscriptionAmount
) {

}
