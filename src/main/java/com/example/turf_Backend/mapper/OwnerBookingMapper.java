package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.DtosProjection.OwnerBookingListProjection;
import com.example.turf_Backend.dto.response.OwnerBookingListItem;
import com.example.turf_Backend.dto.response.OwnerSlotItem;
import com.example.turf_Backend.entity.Slots;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OwnerBookingMapper {
    public OwnerSlotItem mapSlot(Slots s)
    {
        return OwnerSlotItem.builder()
                .id(s.getId())
                .date(s.getDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .price(s.getPrice())
                .status(s.getStatus().name())
                .build();
    }

    public OwnerBookingListItem map(OwnerBookingListProjection p, List<Slots> slotEntities)
    {
        List<OwnerSlotItem> slotDtos=slotEntities.stream()
                .map(this::mapSlot)
                .toList();

        return OwnerBookingListItem.builder()
                .bookingId(p.getBookingId())
                .turfName(p.getTurfName())
                .customerName(p.getCustomerName())
                .customerEmail(p.getCustomerEmail())
                .amount(p.getAmount())
                .status(p.getBookingStatus())
                .createdAt(p.getCreatedAt())
                .slots(slotDtos)
                .build();
    }
}
