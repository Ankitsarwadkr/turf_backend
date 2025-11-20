package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.response.CustomerSlotResponse;
import com.example.turf_Backend.entity.Slots;
import org.springframework.stereotype.Component;

@Component
public class CustomerSlotMapper {
    public CustomerSlotResponse toDto(Slots slots)
    {
        return CustomerSlotResponse.builder()
                .id(slots.getId())
                .date(slots.getDate())
                .startTime(slots.getStartTime())
                .endTime(slots.getEndTime())
                .price(slots.getPrice())
                .build();
    }

}
