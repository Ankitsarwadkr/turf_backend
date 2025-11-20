package com.example.turf_Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class TurfRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @NotBlank
    private String mapUrl;

    @NotBlank
    private String city;

    private String description;

    private String amenities;

    @NotBlank
    private String turfType;

    @NotNull
    private Boolean available;

    private List<MultipartFile> images;
}
