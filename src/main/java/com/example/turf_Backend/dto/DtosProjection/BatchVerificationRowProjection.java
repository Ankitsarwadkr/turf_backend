package com.example.turf_Backend.dto.DtosProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface BatchVerificationRowProjection {
    Long getOwnerId();
    String getOwnerName();
    Long getEarningId();
    String getBookingId();

    Long getTurfId();
    String getTurfName();
    LocalDateTime getSlotEnd();
    BigDecimal getOwnerAmount();
    Integer getBookingAmount();
    Integer getPlatformFee();
    String getRazorpayOrderId();
    String getRazorpayPaymentId();
    LocalDateTime getSettledAt();

}
