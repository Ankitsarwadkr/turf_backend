package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.response.PublicTurfCardResponse;
import com.example.turf_Backend.dto.response.PublicTurfDetailsResponse;
import com.example.turf_Backend.entity.Turf;
import com.example.turf_Backend.entity.TurfSchedule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PublicTurfMapper {
    public PublicTurfCardResponse toCard(Turf turf)
    {
        String thumbnail=turf.getImages().isEmpty()?"/uploads/defaults/no-img.jpg"
                : turf.getImages().getFirst().getFilePath();

        String shortAddress=extractShortAddress(turf.getAddress());

        int startingPrice=(turf.getSchedule()==null || turf.getSchedule().getPriceSlots()==null || turf.getSchedule().getPriceSlots().isEmpty())?0:
                turf.getSchedule().getPriceSlots()
                        .stream()
                        .mapToInt(p->p.getPricePerSlot())
                        .min()
                        .orElse(0);
        return PublicTurfCardResponse.builder()
                .id(turf.getId())
                .name(turf.getName())
                .city(turf.getCity())
                .shortAddress(shortAddress)
                .thumbnailUrl(thumbnail)
                .startingPrice(startingPrice)
                .available(turf.isAvailable())
                .build();
    }
    private String extractShortAddress(String address)
    {
        if (address==null || address.isBlank()) return "";
        String [] parts=address.split(",");
        return parts[0].trim();

    }

    public PublicTurfDetailsResponse toTurfDetails(Turf turf) {
        TurfSchedule schedule=turf.getSchedule();
        return PublicTurfDetailsResponse.builder()
                .id(turf.getId())
                .name(turf.getName())
                .description(turf.getDescription())
                .amenities(turf.getAmenities())
                .turfType(turf.getTurfType())
                .address(turf.getAddress())
                .city(turf.getCity())
                .mapUrl(turf.getMapUrl())
                .images(turf.getImages().stream()
                        .map(img->img.getFilePath()).toList())
                .openTime(schedule !=null ? schedule.getOpenTime():null)
                .closeTime(schedule !=null ? schedule.getCloseTime():null)
                .slotDurationMinutes(schedule !=null ? schedule.getSlotDurationMinutes():0)
                .priceSlots(schedule==null? List.of():schedule.getPriceSlots().stream()
                        .map(p->new PublicTurfDetailsResponse.PriceSlot(
                                p.getStratTime(),
                                p.getEndTime(),
                                p.getPricePerSlot()))
                        .toList())
                .available(turf.isAvailable())
                .build();
    }
}
