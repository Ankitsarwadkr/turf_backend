package com.example.turf_Backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageActionResponse {
    private Long turfId;
    private String message;
    private int changedCount;     // number of images added or deleted
    private int totalImages;
}
