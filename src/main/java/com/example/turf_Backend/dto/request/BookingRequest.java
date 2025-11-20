package com.example.turf_Backend.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BookingRequest {
    private Long turfId;
    private List<Long> slotIds;
}
