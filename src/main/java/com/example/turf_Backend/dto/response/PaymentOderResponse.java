package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentOderResponse {
 private String bookingId;
 private String razorpayOrderId;
 private int amount;
 private String currency;
 private String status;
 private String paymentId;


}
