package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class CustomerBookingListItem {

    private String bookingId;
    private  Long turfId;
    private String turfName;
    private String turfCity;
    private Integer amount;
    private String bookingStatus;
    private String paymentStatus;
    private List<SlotInfo> slots;

    @Data
    @Builder
    public static class SlotInfo
    {
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
    }
}
