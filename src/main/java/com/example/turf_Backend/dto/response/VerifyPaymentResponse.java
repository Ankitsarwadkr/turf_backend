package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyPaymentResponse {
    private String bookingId;
    private String paymentStatus;
    private String message;
    private String paymentId;
}
