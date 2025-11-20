package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.request.TurfUpdateRequest;
import com.example.turf_Backend.dto.response.TurfResponse;
import com.example.turf_Backend.dto.response.TurfUpdateResponse;
import com.example.turf_Backend.entity.Turf;
import com.example.turf_Backend.entity.TurfImage;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TurfMapper {
    public TurfResponse toResponse(Turf turf)
    {
        return  TurfResponse.builder()
                .id(turf.getId())
                .name(turf.getName())
                .address(turf.getAddress())
                .mapUrl(turf.getMapUrl())
                .city(turf.getCity())
                .description(turf.getDescription())
                .amenities(turf.getAmenities())
                .turfType(turf.getTurfType())
                .available(turf.isAvailable())
                .createdAt(turf.getCreatedAt())
                .imageUrls(turf.getImages().stream()
                        .map(TurfImage::getFilePath)
                        .collect(Collectors.toList()))
                .build();
    }
    public void updateTurfRequest(Turf turf, TurfUpdateRequest request)
    {
        if(request.getName()!=null)turf.setName(request.getName());
        if (request.getAddress() != null) turf.setAddress(request.getAddress());
        if (request.getCity() != null) turf.setCity(request.getCity());
        if (request.getMapUrl() != null) turf.setMapUrl(request.getMapUrl());
        if (request.getDescription() != null) turf.setDescription(request.getDescription());
        if (request.getAmenities() != null) turf.setAmenities(request.getAmenities());
        if (request.getTurfType() != null) turf.setTurfType(request.getTurfType());
        if (request.getAvailable() != null) turf.setAvailable(request.getAvailable());
    }
    public TurfUpdateResponse toTurfUpdateResponse(Turf turf, String message)
    {
        return TurfUpdateResponse.builder()
                .id(turf.getId())
                .name(turf.getName())
                .address(turf.getAddress())
                .mapUrl(turf.getMapUrl())
                .city(turf.getCity())
                .description(turf.getDescription())
                .amenities(turf.getAmenities())
                .turfType(turf.getTurfType())
                .available(turf.isAvailable())
                .imageUrls(turf.getImages().stream()
                        .map(TurfImage::getFilePath)
                        .collect(Collectors.toList()))
                .updatedAt(turf.getUpdatedAt())
                .message(message)
                .build();
    }

}
