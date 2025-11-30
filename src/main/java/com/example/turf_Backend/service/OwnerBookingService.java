package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.DtosProjection.OwnerBookingListProjection;
import com.example.turf_Backend.dto.response.OwnerBookingListItem;
import com.example.turf_Backend.entity.Slots;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.mapper.OwnerBookingMapper;
import com.example.turf_Backend.repository.BookingRepository;
import com.example.turf_Backend.repository.SlotsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        Map<String,List<OwnerBookingListProjection>> grouped=rows.stream().collect(Collectors.groupingBy(OwnerBookingListProjection::getBookingId));
        List<OwnerBookingListItem> result=new ArrayList<>();
        for (String bookingId : grouped.keySet())
        {
            List<OwnerBookingListProjection> list=grouped.get(bookingId);
            List<Long> slotIds=list.stream()
                    .map(OwnerBookingListProjection::getSlotId)
                    .toList();

            List<Slots> slotEntites=slotsRepository.findAllByIds(slotIds);

            OwnerBookingListProjection first= list.get(0);

            result.add(mapper.map(first,slotEntites));
         }
        return result;
    }
}
