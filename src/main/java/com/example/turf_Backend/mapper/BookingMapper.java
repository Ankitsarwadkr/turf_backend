package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.DtosProjection.CustomerBookingDetailsProjection;
import com.example.turf_Backend.dto.DtosProjection.CustomerBookingListProjection;
import com.example.turf_Backend.dto.response.BookingResponse;
import com.example.turf_Backend.dto.response.CustomerBookingDetails;
import com.example.turf_Backend.dto.response.CustomerBookingListItem;
import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.entity.Slots;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {
    public BookingResponse toDto(Booking booking, List<Slots> slotEntites,String message)
    {
        int slotTotal=slotEntites.stream()
                .mapToInt(Slots::getPrice)
                .sum();

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
                .slotTotal(slotTotal)
                .platformFee(booking.getPlatformFee())
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
    public CustomerBookingListItem map(CustomerBookingListProjection row,List<Slots> slotEntities) {
        return CustomerBookingListItem.builder()
                .bookingId(row.getBookingId())
                .turfId(row.getTurfId())
                .turfName(row.getTurfName())
                .turfCity(row.getTurfCity())
                .amount(row.getAmount())
                .bookingStatus(row.getBookingStatus())
                .paymentStatus(resolvePaymentStatus(row))
                .slots(
                        slotEntities.stream().map( s->
                                CustomerBookingListItem.SlotInfo.builder()
                                        .date(s.getDate())
                                        .startTime(s.getStartTime())
                                        .endTime(s.getEndTime())
                                        .build()
                        ).toList()
                )
                .build();
    }

    private String resolvePaymentStatus(CustomerBookingListProjection row)
    {
        if (row.getPaymentStatus()==null)
        {
            return "PENDING";
        }
        return row.getPaymentStatus();
    }

    public CustomerBookingDetails mapToDto(CustomerBookingDetailsProjection row,List<Slots> slotEntities,String turfImage)
    {
        return CustomerBookingDetails.builder()
                .bookingId(row.getBookingId())
                .turfId(row.getTurfId())
                .turfName(row.getTurfName())
                .turfCity(row.getTurfCity())
                .turfAddress(row.getTurfAddress())
                .turfImage(turfImage)
                .amount(row.getAmount())
                .bookingStatus(row.getBookingStatus())
                .paymentStatus(resolvePaymentStatusForBookingDetails(row))
                .paymentId(row.getPaymentId())
                .createdAt(row.getCreatedAt())
                .expireAt(row.getExpireAt())
                .slots(
                        slotEntities.stream().map(s->
                                CustomerBookingDetails.SlotInfo.builder()
                                        .date(s.getDate())
                                        .startTime(s.getStartTime())
                                        .endTime(s.getEndTime())
                                        .price(s.getPrice())
                                        .build() ).toList()
                )
                .build();
    }
    private String resolvePaymentStatusForBookingDetails(CustomerBookingDetailsProjection row)
    {
        if (row.getPaymentStatus()==null)
        {
            return "PENDING";
        }
        return row.getPaymentStatus();
    }
}
