package com.example.turf_Backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurfUpdateResponse {
    private Long id;
    private String name;
    private String address;
    private String mapUrl;
    private String locality;
    private String city;
    private String description;
    private String amenities;
    private String turfType;
    private boolean available;
    private List<String> imageUrls;
    private LocalDateTime updatedAt;
    private String message;
}
