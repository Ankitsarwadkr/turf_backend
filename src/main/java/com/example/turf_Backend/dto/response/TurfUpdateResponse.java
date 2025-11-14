package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TurfUpdateResponse {
    private Long id;
    private String name;
    private String address;
    private String location;
    private String city;
    private String description;
    private String amenities;
    private String turfType;
    private boolean available;
    private List<String> imageUrls;
    private LocalDateTime updatedAt;
    private String message;
}
