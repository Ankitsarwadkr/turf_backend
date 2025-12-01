package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.DtosProjection.OwnerBookingDetailsProjection;
import com.example.turf_Backend.dto.DtosProjection.OwnerBookingListProjection;
import com.example.turf_Backend.dto.response.OwnerBookingDetailsResponse;
import com.example.turf_Backend.dto.response.OwnerBookingListItem;
import com.example.turf_Backend.dto.response.OwnerSlotItem;
import com.example.turf_Backend.entity.Slots;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.OwnerBookingMapper;
import com.example.turf_Backend.repository.BookingRepository;
import com.example.turf_Backend.repository.SlotsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerBookingService {
    private final BookingRepository bookingRepository;
    private final SlotsRepository slotsRepository;
    private final OwnerBookingMapper mapper;


    public List<OwnerBookingListItem> getBookingListForOwner() {
        User owner=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long ownerId= owner.getId();

        List<OwnerBookingListProjection> rows=bookingRepository.findBookingRows(ownerId);

        if (rows.isEmpty()) return List.of();

        Map<String,List<OwnerBookingListProjection>> grouped=rows.stream()
                .collect(Collectors.groupingBy(OwnerBookingListProjection::getBookingId
                , LinkedHashMap::new,Collectors.toList()));

         return grouped.values().stream()
                 .map(group->{
                     OwnerBookingListProjection first=group.get(0);

                     List<OwnerSlotItem> slots=group.stream()
                             .map(row->OwnerSlotItem.builder()
                                     .id(row.getSlotId())
                                     .date(row.getSlotDate())
                                     .startTime(row.getSlotStartTime())
                                     .endTime(row.getSlotEndTime())
                                     .price(row.getSlotPrice())
                                     .status(row.getSlotStatus())
                                     .build())
                             .toList();
                     return OwnerBookingListItem.builder()
                             .bookingId(first.getBookingId())
                             .turfName(first.getTurfName())
                             .customerName(first.getCustomerName())
                             .customerEmail(first.getCustomerEmail())
                             .amount(first.getAmount())
                             .status(first.getBookingStatus())
                             .createdAt(first.getCreatedAt())
                             .slots(slots)
                             .build();
                 })
                 .toList();
    }

    public OwnerBookingDetailsResponse getBookingDetailsForOwner(String bookingId) {
        User owner=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long ownerId= owner.getId();

        List<OwnerBookingDetailsProjection> rows=bookingRepository.findOwnerBookingDetails(bookingId,ownerId);

        if (rows.isEmpty())
        {
            throw new CustomException("Booking not Found", HttpStatus.NOT_FOUND);
        }

        OwnerBookingDetailsProjection first=rows.get(0);
        List<OwnerBookingDetailsResponse.SlotInfo> slots=rows.stream()
                .map(row->OwnerBookingDetailsResponse.SlotInfo.builder()
                        .date(row.getSlotDate().toString())
                        .startTime(row.getSlotStartTime().toString())
                        .endTime(row.getSlotEndTime().toString())
                        .price(row.getSlotPrice())
                        .build())
                .toList();
        return OwnerBookingDetailsResponse.builder()
                .bookingId(first.getBookingId())
                .turf(OwnerBookingDetailsResponse.TurfInfo.builder()
                        .id(first.getTurfId())
                        .name(first.getTurfName())
                        .city(first.getTurfCity())
                        .address(first.getTurfAddress())
                        .image(first.getTurfImage())
                        .build())
                .customer(OwnerBookingDetailsResponse.CustomerInfo.builder()
                        .name(first.getCustomerName())
                        .email(first.getCustomerEmail())
                        .build())
                .amount(first.getAmount())
                .status(first.getBookingStatus())
                .createdAt(first.getCreatedAt())
                .slots(slots)
                .build();
    }
}
