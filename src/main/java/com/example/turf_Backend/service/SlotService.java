package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.SlotStatusUpdateRequest;
import com.example.turf_Backend.dto.response.SlotResponse;
import com.example.turf_Backend.entity.*;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.repository.SlotsRepository;
import com.example.turf_Backend.repository.TurfRepository;
import com.example.turf_Backend.repository.TurfScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotService {
    private  final TurfRepository turfRepository;
    private final TurfScheduleRepository turfScheduleRepository;
    private final SlotsRepository slotsRepository;

    @Transactional
    public void generateSlots(Long turfId, int days) {
        User owner=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf=turfRepository.findById(turfId).orElseThrow(()->new CustomException("Turf not Found ", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId()))
        {
            throw  new CustomException("Unauthorized to generate slots for this turf ",HttpStatus.FORBIDDEN);
        }
        TurfSchedule schedule=turfScheduleRepository.findByTurfId(turfId).orElseThrow(()->new CustomException("Turf Schedule not found ",HttpStatus.NOT_FOUND));

        List<TurfPriceSlot> priceSlots=schedule.getPriceSlots();
        if (priceSlots.isEmpty()) throw new CustomException("No price slots configured for this turf",HttpStatus.BAD_REQUEST);

        int slotDuration= schedule.getSlotDurationMinutes();

        ZoneId zone = ZoneId.of("Asia/Kolkata"); // or dynamic later per turf location
        for (int dayOffset = 0; dayOffset < days; dayOffset++) {
            LocalDate date = LocalDate.now(zone).plusDays(dayOffset);
            generateForDay(turf, schedule, priceSlots, date, slotDuration);
        }
        log.info("Completed slot generation for turf '{}' by '{}' — {} days processed", turf.getName(), owner.getEmail(), days);
    }
    public void generateForDay(Turf turf,TurfSchedule schedule,List<TurfPriceSlot> priceSlots,LocalDate date,int duration)
    {
        LocalTime open=schedule.getOpenTime();
        LocalTime close=schedule.getCloseTime();
        boolean is24hr = open.equals(close);
        if (is24hr) {
            // Treat as open full day (00:00–23:59)
            close = open.minusMinutes(1);
            log.info("Turf '{}' is open 24 hours; generating full-day slots", turf.getName());
        }
        //Handle Cross Midnight (Eg 7am to 1am)
        boolean crossesMidnight=close.isBefore(open);
        LocalDateTime current=date.atTime(open);
        LocalDateTime endBoundary=crossesMidnight
                ? date.plusDays(1).atTime(close)
                : date.atTime(close);

        List<Slots> newSlots = new ArrayList<>();
        while (current.plusMinutes(duration).isBefore(endBoundary)||current.plusMinutes(duration).equals(endBoundary))
        {
            LocalDateTime slotEnd=current.plusMinutes(duration);
            LocalTime startTime=current.toLocalTime();
            LocalTime endTime=slotEnd.toLocalTime();

            //find price slot for this start time
             TurfPriceSlot priceSlot=priceSlots.stream()
                     .filter(p -> isWithinTimeRange(startTime, p.getStratTime(), p.getEndTime()))
                     .findFirst()
                     .orElse(null);

            if (priceSlot == null) {
                log.warn(" No price slot found for turf {} at {}", turf.getName(), startTime);
                current = slotEnd;
                continue;
            }

            int price = priceSlot.getPricePerSlot();

            // Skip if slot already exists
            boolean exists = slotsRepository.existsByTurfAndDateAndStartTimeAndEndTime(turf, date, startTime, endTime);
            if (exists) {
                current = slotEnd;
                continue;
            }
            newSlots.add(Slots.builder()
                    .turf(turf)
                    .date(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .price(price)
                    .status(SlotStatus.AVAILABLE)
                    .build());
            current = slotEnd;
        }

        if (!newSlots.isEmpty()) {
            slotsRepository.saveAll(newSlots);
        }
        log.info("Generated {} slots for turf '{}' on {}", newSlots.size(), turf.getName(), date);
    }

    private boolean isWithinTimeRange(LocalTime target, LocalTime start, LocalTime end) {
        if (end.isAfter(start)) {
            return !target.isBefore(start) && target.isBefore(end);
        } else {
            // Cross-midnight range
            return !target.isBefore(start) || target.isBefore(end);
        }
        }

    public List<SlotResponse> getSlotsForTurf(Long turfId, LocalDate date) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new CustomException("Turf not found", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw new CustomException("Unauthorized access to this turf", HttpStatus.FORBIDDEN);
        }

        List<Slots> slots=slotsRepository.findByTurfIdAndDateOrderByStartTime(turfId,date);

        if (slots.isEmpty())
        {
            throw new CustomException("No slots found for the given date", HttpStatus.NOT_FOUND);
        }
        return slots.stream().map(
                s->SlotResponse.builder()
                        .id(s.getId())
                        .date(s.getDate())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .price(s.getPrice())
                        .status(s.getStatus().name())
                        .build())
                .toList();
    }

    @Transactional
    public void updateSlotStatus(Long turfId, SlotStatusUpdateRequest request) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new CustomException("Turf not found", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw new CustomException("Unauthorized access to this turf", HttpStatus.FORBIDDEN);
        }
        SlotStatus newStatus;
        if (request == null) {
            throw new CustomException("Request body is missing", HttpStatus.BAD_REQUEST);
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            throw new CustomException("Status field is required", HttpStatus.BAD_REQUEST);
        }
        try {
            newStatus = SlotStatus.valueOf(request.getStatus().toUpperCase());

        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid status value. Allowed: AVAILABLE, UNAVAILABLE", HttpStatus.BAD_REQUEST);
        }

        if(newStatus==SlotStatus.BOOKED)
        {
            throw new CustomException("Cannot manually set slots status to booked",HttpStatus.BAD_REQUEST);
        }
        List<Slots> slots=slotsRepository.findAllById(request.getSlotIds());
        if(slots.isEmpty())
        {
            throw new CustomException("No Slots found for provided ids",HttpStatus.NOT_FOUND);
        }
        int updatedCount=0;
        for (Slots slot : slots) {
            if (!slot.getTurf().getId().equals(turfId)) continue;

            if (slot.getStatus() == SlotStatus.BOOKED) {
                log.warn("Skipping slot {} as it is already BOOKED", slot.getId());
                continue;
            }

            if (slot.getStatus() != newStatus) {
                slot.setStatus(newStatus);
                updatedCount++;
            }
        }
        if (updatedCount > 0) {
            slotsRepository.saveAll(slots);
        }

        log.info("Updated {} slots to status {} for turf '{}' by '{}'", updatedCount, newStatus, turf.getName(), owner.getEmail());
    }
}
