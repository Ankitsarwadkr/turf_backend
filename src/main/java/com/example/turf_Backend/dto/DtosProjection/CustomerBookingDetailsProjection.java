package com.example.turf_Backend.dto.DtosProjection;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerBookingDetailsProjection {
    String getBookingId();
    Long getTurfId();
    String getTurfName();
    String getTurfCity();
    String getTurfAddress();
    Integer getAmount();
    String getBookingStatus();

    String getPaymentStatus();
    String getPaymentId();

    List<Long> getSlotId();
    LocalDateTime getCreatedAt();
    LocalDateTime getExpireAt();
}
