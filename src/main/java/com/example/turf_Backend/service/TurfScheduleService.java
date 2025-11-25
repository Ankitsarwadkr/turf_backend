package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.TurfScheduleRequest;
import com.example.turf_Backend.dto.response.TurfScheduleResponse;
import com.example.turf_Backend.entity.Turf;
import com.example.turf_Backend.entity.TurfPriceSlot;
import com.example.turf_Backend.entity.TurfSchedule;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.TurfScheduleMapper;
import com.example.turf_Backend.repository.SlotsRepository;
import com.example.turf_Backend.repository.TurfRepository;
import com.example.turf_Backend.repository.TurfScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TurfScheduleService {
    private  final TurfRepository turfRepository;
    private final TurfScheduleRepository turfScheduleRepository;
    private final TurfScheduleMapper turfScheduleMapper;
    private final SlotsRepository slotsRepository;
    private static final Set<Integer> ALLOWED_DURATIONS = Set.of(60, 90, 120);
    private static final int MINUTES_PER_DAY = 24 * 60;

    @Transactional
    public TurfScheduleResponse createOrUpdateSchedule(Long turfId, TurfScheduleRequest request) {
        User owner= (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf=turfRepository.findById(turfId).orElseThrow(()->new CustomException("Turf not Found ", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId()))
        {
            throw new CustomException("Unauthorized to modify this turf",HttpStatus.FORBIDDEN);
        }

        //Block update if any booking exists

        boolean hasFutureBookings = slotsRepository.existsFutureBooking(turfId);
        if (hasFutureBookings) {
            throw new CustomException(
                    "Cannot update schedule because future bookings already exist. " +
                            "Cancel upcoming bookings first.",
                    HttpStatus.CONFLICT
            );
        }
        if (request.getPriceSlots() == null || request.getPriceSlots().isEmpty()) {
            throw new CustomException("At least one price slot must be provided", HttpStatus.BAD_REQUEST);
        }
        if (request.getOpenTime() == null || request.getCloseTime() == null) {
            throw new CustomException("openTime and closeTime are required", HttpStatus.BAD_REQUEST);
        }
        if (request.getSlotDurationMinutes() <= 0) {
            throw new CustomException("slotDurationMinutes must be > 0", HttpStatus.BAD_REQUEST);
        }
        if (!ALLOWED_DURATIONS.contains(request.getSlotDurationMinutes())) {
            throw new CustomException("Unsupported slot duration. Allowed: " + ALLOWED_DURATIONS, HttpStatus.BAD_REQUEST);
        }
        int open = toMinutes(request.getOpenTime());
        int close = toMinutes(request.getCloseTime());
        if (close <= open) close += MINUTES_PER_DAY;
        validatePriceSlotsAndCoverage(request.getPriceSlots(), open, close);
        if (open + request.getSlotDurationMinutes() > close) {
            throw new CustomException("slot duration too large for the open-close window", HttpStatus.BAD_REQUEST);
        }

        TurfSchedule schedule = turfScheduleRepository.findByTurfId(turfId).orElse(null);

        if (schedule == null) {
            schedule = turfScheduleMapper.toEntity(request);
            schedule.setTurf(turf);
        } else {
            schedule.setOpenTime(request.getOpenTime());
            schedule.setCloseTime(request.getCloseTime());
            schedule.setSlotDurationMinutes(request.getSlotDurationMinutes());

            // rebuild price slots properly and attach schedule
            schedule.getPriceSlots().clear();
            for (TurfScheduleRequest.PriceSlotRequest ps : request.getPriceSlots()) {
                schedule.getPriceSlots().add(
                        TurfPriceSlot.builder()
                                .stratTime(ps.getStartTime())
                                .endTime(ps.getEndTime())
                                .pricePerSlot(ps.getPricePerSlot())
                                .schedule(schedule)
                                .build()
                );
            }
        }

        TurfSchedule saved = turfScheduleRepository.save(schedule);
        log.info("Schedule saved for turf {} ", turf.getName());
        return turfScheduleMapper.toResponse(saved, "Turf Schedule saved successfully");
    }

    private void validatePriceSlotsAndCoverage(List<TurfScheduleRequest.PriceSlotRequest> slots, int open, int close) {
        record Interval(int start, int end, String repr) {}

        // Normalize intervals and map to minute ranges that can exceed 1440 to represent next-day end
        List<Interval> normalized = slots.stream()
                .map(s -> {
                    int sStart = toMinutes(s.getStartTime());
                    int sEnd = toMinutes(s.getEndTime());
                    if (sEnd <= sStart) sEnd += MINUTES_PER_DAY; // cross-midnight convert (e.g. 16:00-01:00 -> 960..1500)
                    return new Interval(sStart, sEnd, s.getStartTime() + "-" + s.getEndTime());
                })
                .sorted(Comparator.comparingInt(Interval::start)) // MUST sort by start
                .collect(Collectors.toList());

        // Quick: every normalized interval must lie within [open, close)
        for (Interval it : normalized) {
            if (it.start < open || it.end > close) {
                throw new CustomException("Price slot " + it.repr() + " is outside open-close range", HttpStatus.BAD_REQUEST);
            }
        }

        // No overlaps and no gaps: intervals must be contiguous and non-overlapping.
        for (int i = 0; i < normalized.size() - 1; i++) {
            Interval a = normalized.get(i);
            Interval b = normalized.get(i + 1);
            // overlap if a.end > b.start
            if (a.end > b.start) {
                throw new CustomException("Overlapping price slots: " + a.repr() + " and " + b.repr(), HttpStatus.BAD_REQUEST);
            }
            // gap if a.end < b.start
            if (a.end < b.start) {
                throw new CustomException("Gap detected between price slots: " + a.repr() + " and " + b.repr(), HttpStatus.BAD_REQUEST);
            }
            // if equal it's contiguous, that's OK
        }

        // First slot must start at open; last slot must end at close
        if (normalized.isEmpty()) {
            throw new CustomException("At least one price slot is required", HttpStatus.BAD_REQUEST);
        }
        Interval first = normalized.get(0);
        Interval last = normalized.get(normalized.size() - 1);

        if (first.start != open) {
            throw new CustomException("Price slots must start at open time", HttpStatus.BAD_REQUEST);
        }
        if (last.end != close) {
            throw new CustomException("Price slots must end at close time", HttpStatus.BAD_REQUEST);
        }
    }
    private static int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }
}
