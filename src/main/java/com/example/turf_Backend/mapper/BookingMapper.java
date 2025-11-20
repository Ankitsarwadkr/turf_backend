package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.response.BookingResponse;
import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.entity.Slots;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {
    public BookingResponse toDto(Booking booking, List<Slots> slotEntites,String message)
    {
        List<BookingResponse.SlotInfo>slotInfos=slotEntites.stream()
                .map(s->BookingResponse.SlotInfo.builder()
                        .slotId(s.getId())
                        .date(s.getDate())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .price(s.getPrice())
                        .build())
                .toList();

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .amount(booking.getAmount())
                .status(booking.getStatus().name())
                .expiredAt(booking.getExpireAt())
                .turfId(booking.getTurf().getId())
                .turfName(booking.getTurf().getName())
                .turfCity(booking.getTurf().getCity())
                .slots(slotInfos)
                .message(message)
                .build();
    }

}
