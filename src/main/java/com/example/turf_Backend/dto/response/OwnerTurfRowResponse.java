package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OwnerTurfRowResponse {
    private Long id;
    private String name;
    private String city;
    private String turfType;
    private boolean bookingEnabled;
    private String primaryImage;
    private LocalDateTime createdAt;
}
