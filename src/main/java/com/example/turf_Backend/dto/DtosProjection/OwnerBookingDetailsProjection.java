package com.example.turf_Backend.dto.DtosProjection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface OwnerBookingDetailsProjection {
    String getBookingId();
    Long getTurfId();
    String getTurfName();
    String getTurfCity();
    String getTurfAddress();
    String getTurfImage();

    String getCustomerName();
    String getCustomerEmail();

    Integer getAmount();
    String getBookingStatus();

    LocalDateTime getCreatedAt();

    LocalDate getSlotDate();
    LocalTime getSlotStartTime();
    LocalTime getSlotEndTime();
    Integer getSlotPrice();

}
