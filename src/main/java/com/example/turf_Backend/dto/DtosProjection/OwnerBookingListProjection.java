package com.example.turf_Backend.dto.DtosProjection;

import java.util.List;

public interface OwnerBookingListProjection {
    String getBookingId();

    Long getTurfId();
    String getTurfName();

    Long getCustomerId();
    String getCustomerName();
    String getCustomerEmail();

    Integer getAmount();
    String getBookingStatus();
    java.time.LocalDateTime getCreatedAt();
    Long getSlotId();
}
