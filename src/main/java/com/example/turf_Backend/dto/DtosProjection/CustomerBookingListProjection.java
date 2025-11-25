package com.example.turf_Backend.dto.DtosProjection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface CustomerBookingListProjection {
    String getBookingId();
    Long getTurfId();
    String getTurfName();
    String getTurfCity();
    Integer getAmount();
    String getBookingStatus();
    String getPaymentStatus();
    List<Long> getSlotId();

}
