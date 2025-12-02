package com.example.turf_Backend.dto.request;

import lombok.Data;

@Data
public class VerifyPaymentRequest {
    private String bookingId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
