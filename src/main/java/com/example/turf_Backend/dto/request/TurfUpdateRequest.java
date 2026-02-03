package com.example.turf_Backend.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class TurfUpdateRequest {
    private String name;
    private String address;
    private String mapUrl;
    private String locality;
    private String city;
    private String description;
    private String amenities;
    private String turfType;
    private Boolean available;

}
