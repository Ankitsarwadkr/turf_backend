package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.response.CustomerSlotResponse;
import com.example.turf_Backend.enums.SlotStatus;
import com.example.turf_Backend.entity.Slots;
import com.example.turf_Backend.entity.Turf;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.CustomerSlotMapper;
import com.example.turf_Backend.repository.SlotsRepository;
import com.example.turf_Backend.repository.TurfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerSlotService {
    private final SlotsRepository slotsRepository;
    private final TurfRepository turfRepository;
    private final CustomerSlotMapper customerSlotMapper;

    public List<CustomerSlotResponse> getAvailableSlots(Long turfId, LocalDate date) {
        if (date.isBefore(LocalDate.now()))
        {
            throw new CustomException("Cannot view past date slots", HttpStatus.BAD_REQUEST);
        }
        Turf turf=turfRepository.findById(turfId).orElseThrow(()->new CustomException("Turf Not Found ",HttpStatus.NOT_FOUND));
        List<Slots> slots=slotsRepository.findByTurfIdAndDateOrderByStartTime(turfId,date);

        if (slots.isEmpty())
        {
            throw new CustomException("No Slots configured for this date ",HttpStatus.NOT_FOUND);
        }
        LocalDate today=LocalDate.now();
        LocalTime now=LocalTime.now();

        List<Slots> filtered=slots.stream()
                .filter(s->s.getStatus()== SlotStatus.AVAILABLE)
                .filter(s->{
                    if(!date.equals(today)) return true;
                return s.getStartTime().isAfter(now);
                })
                .toList();

        if (filtered.isEmpty())
        {
            throw new CustomException("All Slots are booked for this date ",HttpStatus.NOT_FOUND);
        }
        return filtered.stream()
                .map(customerSlotMapper::toDto)
                .toList();

    }

}
