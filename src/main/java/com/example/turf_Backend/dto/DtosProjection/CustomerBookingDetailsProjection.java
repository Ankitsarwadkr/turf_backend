package com.example.turf_Backend.dto.DtosProjection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface CustomerBookingDetailsProjection {
    String getBookingId();
    Long getTurfId();
    String getTurfName();
    String getTurfCity();
    String getTurfAddress();
    String getTurfImage();
    Integer getAmount();
    String getBookingStatus();
    String getPaymentStatus();
    String getPaymentId();
    LocalDateTime getCreatedAt();
    LocalDateTime getExpireAt();
    LocalDate getSlotDate();
    LocalTime getSlotStartTime();
    LocalTime getSlotEndTime();
    Integer getSlotPrice();
}
