package com.example.turf_Backend.dto.response;

import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurfScheduleResponse {
    private Long turfId;
    private LocalTime openTime;
    private  LocalTime closeTime;
    private int slotDurationMinutes;

    private List<PriceSlotResponse> priceSlots;
    private String message;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static  class PriceSlotResponse{

        private LocalTime startTime;
        private LocalTime endTime;
        private int pricePerSlot;
    }


}
