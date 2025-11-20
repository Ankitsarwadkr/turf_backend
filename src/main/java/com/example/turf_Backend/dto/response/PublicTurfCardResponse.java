package com.example.turf_Backend.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicTurfCardResponse {
    private Long id;
    private String name;
    private String city;
    private String shortAddress;
    private String thumbnailUrl;
    private int startingPrice;
    private boolean available;
}
