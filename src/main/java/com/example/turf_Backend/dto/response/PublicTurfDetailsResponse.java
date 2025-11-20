package com.example.turf_Backend.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicTurfDetailsResponse {
    private Long id;
    private String name;
    private String description;
    private String amenities;
    private String turfType;
    private String address;
    private String city;
    private String mapUrl;
    private List<String> images;
    private LocalTime openTime;
    private LocalTime closeTime;
    private int slotDurationMinutes;
    private List<PriceSlot> priceSlots;
    private boolean available;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceSlot
    {
        private LocalTime startTime;
        private LocalTime endTime;
        private int pricePerSlot;
    }

}
