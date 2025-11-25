package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.SlotStatusUpdateRequest;
import com.example.turf_Backend.dto.response.SlotResponse;
import com.example.turf_Backend.entity.*;
import com.example.turf_Backend.enums.SlotStatus;
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

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotService {

    private final TurfRepository turfRepository;
    private final TurfScheduleRepository turfScheduleRepository;
    private final SlotsRepository slotsRepository;

    private static final int MINUTES_PER_DAY = 1440;


    @Transactional
    public void generateSlots(Long turfId, int days) {

        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new CustomException("Turf not Found", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw new CustomException("Unauthorized to generate slots for this turf", HttpStatus.FORBIDDEN);
        }

        TurfSchedule schedule = turfScheduleRepository.findByTurfId(turfId)
                .orElseThrow(() -> new CustomException("Turf Schedule not found", HttpStatus.NOT_FOUND));

        List<TurfPriceSlot> priceSlots = schedule.getPriceSlots();
        if (priceSlots.isEmpty()) {
            throw new CustomException("No price slots configured for this turf", HttpStatus.BAD_REQUEST);
        }

        int duration = schedule.getSlotDurationMinutes();
        ZoneId zone = ZoneId.of("Asia/Kolkata");

        for (int offset = 0; offset < days; offset++) {
            LocalDate date = LocalDate.now(zone).plusDays(offset);
            generateSlotsForSingleDay(turf, schedule, priceSlots, date, duration);
        }

        log.info("Completed slot generation for turf '{}' for {} days", turf.getName(), days);
    }


    private void generateSlotsForSingleDay(
            Turf turf,
            TurfSchedule schedule,
            List<TurfPriceSlot> priceSlots,
            LocalDate date,
            int duration
    ) {
        LocalTime open = schedule.getOpenTime();
        LocalTime close = schedule.getCloseTime();

        LocalDateTime start = date.atTime(open);
        LocalDateTime end;

        // Case 1: 24-hour open (open == close)
        if (open.equals(close)) {
            end = date.plusDays(1).atTime(open);
            log.info("Turf '{}' is 24 hours open", turf.getName());
        }
        // Case 2: Cross midnight close (e.g. 7am -> 1am)
        else if (close.isBefore(open)) {
            end = date.plusDays(1).atTime(close);
        }
        // Case 3: Same-day close
        else {
            end = date.atTime(close);
        }

        List<Slots> newSlots = new ArrayList<>();
        LocalDateTime current = start;

        while (!current.plusMinutes(duration).isAfter(end)) {

            LocalDateTime slotEnd = current.plusMinutes(duration);
            LocalTime startTime = current.toLocalTime();
            LocalTime endTime = slotEnd.toLocalTime();

            TurfPriceSlot priceSlot = matchPriceSlot(startTime, priceSlots);

            if (priceSlot == null) {
                log.warn("No price slot found for {}", startTime);
                current = slotEnd;
                continue;
            }

            boolean exists = slotsRepository.existsByTurfAndDateAndStartTimeAndEndTime(
                    turf, date, startTime, endTime);

            if (!exists) {
                newSlots.add(Slots.builder()
                        .turf(turf)
                        .date(date)
                        .startTime(startTime)
                        .endTime(endTime)
                        .price(priceSlot.getPricePerSlot())
                        .status(SlotStatus.AVAILABLE)
                        .build());
            }

            current = slotEnd;
        }

        if (!newSlots.isEmpty()) {
            slotsRepository.saveAll(newSlots);
        }

        log.info("Generated {} slots for turf '{}' on {}", newSlots.size(), turf.getName(), date);
    }


    //  FIXED PRICE SLOT MATCHING (MINUTE NORMALIZED)
    private TurfPriceSlot matchPriceSlot(LocalTime t, List<TurfPriceSlot> priceSlots) {
        int tm = toMinutes(t);

        for (TurfPriceSlot p : priceSlots) {
            int sm = toMinutes(p.getStratTime());
            int em = toMinutes(p.getEndTime());

            if (em <= sm) em += MINUTES_PER_DAY;  // cross midnight fix
            int tt = tm;
            if (tm < sm) tt += MINUTES_PER_DAY;   // target also adjusted if past midnight

            if (tt >= sm && tt < em) {
                return p;
            }
        }

        return null;
    }

    private int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }


    //  GET SLOTS FOR A DAY

    public List<SlotResponse> getSlotsForTurf(Long turfId, LocalDate date) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new CustomException("Turf not found", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw new CustomException("Unauthorized access to this turf", HttpStatus.FORBIDDEN);
        }

        List<Slots> slots = slotsRepository.findByTurfIdAndDateOrderByStartTime(turfId, date);
        if (slots.isEmpty()) {
            throw new CustomException("No slots found for the given date", HttpStatus.NOT_FOUND);
        }

        List<SlotResponse> responses = new ArrayList<>();
        for (Slots s : slots) {
            responses.add(SlotResponse.builder()
                    .id(s.getId())
                    .date(s.getDate())
                    .startTime(s.getStartTime())
                    .endTime(s.getEndTime())
                    .price(s.getPrice())
                    .status(s.getStatus().name())
                    .build());
        }

        return responses;
    }

    // ------------------------------------------------------------
    //  UPDATE SLOT STATUS
    // ------------------------------------------------------------
    @Transactional
    public void updateSlotStatus(Long turfId, SlotStatusUpdateRequest request) {

        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new CustomException("Turf not found", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw new CustomException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        if (request == null) throw new CustomException("Request body missing", HttpStatus.BAD_REQUEST);
        if (request.getStatus() == null) throw new CustomException("Status required", HttpStatus.BAD_REQUEST);

        SlotStatus newStatus;
        try {
            newStatus = SlotStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new CustomException("Invalid status", HttpStatus.BAD_REQUEST);
        }

        if (newStatus == SlotStatus.BOOKED) {
            throw new CustomException("Cannot manually set BOOKED", HttpStatus.BAD_REQUEST);
        }

        List<Slots> slots = slotsRepository.findAllById(request.getSlotIds());

        if (slots.isEmpty()) {
            throw new CustomException("No slots found", HttpStatus.NOT_FOUND);
        }

        int updated = 0;
        for (Slots s : slots) {
            if (!s.getTurf().getId().equals(turfId)) continue;
            if (s.getStatus() == SlotStatus.BOOKED) continue;

            if (s.getStatus() != newStatus) {
                s.setStatus(newStatus);
                updated++;
            }
        }

        if (updated > 0) slotsRepository.saveAll(slots);

        log.info("Updated {} slots to {}", updated, newStatus);
    }
}
