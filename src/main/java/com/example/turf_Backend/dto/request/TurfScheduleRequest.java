package com.example.turf_Backend.dto.request;

import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurfScheduleRequest {

    private LocalTime openTime;
    private LocalTime closeTime;
    private int slotDurationMinutes;

    private List<PriceSlotRequest> priceSlots;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceSlotRequest{
        private LocalTime startTime;
        private LocalTime endTime;
        private int pricePerSlot;
    }

}
