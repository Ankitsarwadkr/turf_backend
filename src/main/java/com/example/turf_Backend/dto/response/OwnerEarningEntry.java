package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OwnerEarningEntry {
    private Long earningId;
    private String bookingId;
    private Long turfId;
    private String turfName;
    private LocalDateTime slotEnd;
    private BigDecimal bookingAmount;
    private BigDecimal platformFee;
    private BigDecimal ownerAmount;
    private String razorpayPaymentId;
    private  String razorpayOderId;
    private LocalDateTime settledAt;

}
