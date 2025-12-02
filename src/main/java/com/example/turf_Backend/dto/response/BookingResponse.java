package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class BookingResponse {
    private String bookingId;
    private Long turfId;
    private List<SlotInfo> slots;
    private int slotTotal;
    private int platformFee;
    private int amount;//total payable by customer
    private String status;
    private String turfName;
    private String turfCity;

    private LocalDateTime expiredAt;
    private String message;

    @Data
    @Builder
    public static class SlotInfo {
        private Long slotId;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private int price;

    }

}
