package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class CustomerBookingDetails {
    private String bookingId;
    private Long turfId;
    private String turfName;
    private String turfCity;
    private String turfAddress;
    private String turfImage;

    private Integer amount;
    private String bookingStatus;
    private String paymentStatus;
    private String paymentId;

    private List<SlotInfo>slots;
    private LocalDateTime createdAt;
    private LocalDateTime  expireAt;

    @Data
    @Builder
    public static class SlotInfo{
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer price;
    }
}
