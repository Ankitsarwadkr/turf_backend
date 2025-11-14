package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.TurfScheduleRequest;
import com.example.turf_Backend.dto.response.TurfScheduleResponse;
import com.example.turf_Backend.entity.Turf;
import com.example.turf_Backend.entity.TurfSchedule;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.TurfScheduleMapper;
import com.example.turf_Backend.repository.TurfRepository;
import com.example.turf_Backend.repository.TurfScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TurfScheduleService {
    private  final TurfRepository turfRepository;
    private final TurfScheduleRepository turfScheduleRepository;
    private final TurfScheduleMapper turfScheduleMapper;

    @Transactional
    public TurfScheduleResponse createOrUpdateSchedule(Long turfId, TurfScheduleRequest request) {
        User owner= (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf=turfRepository.findById(turfId).orElseThrow(()->new CustomException("Turf not Found ", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId()))
        {
            throw new CustomException("Unauthorized to modify this turf",HttpStatus.FORBIDDEN);
        }
        validatePriceSlots(request.getPriceSlots());
        TurfSchedule schedule=turfScheduleRepository.findByTurfId(turfId).orElse(null);
        if (schedule==null)
        {
            schedule=turfScheduleMapper.toEntity(request);
            schedule.setTurf(turf);
        }
        else {
            schedule.setOpenTime(request.getOpenTime());
            schedule.setCloseTime(request.getCloseTime());
            schedule.setSlotDurationMinutes(request.getSlotDurationMinutes());
            schedule.getPriceSlots().clear();
            schedule.getPriceSlots().addAll(
                    turfScheduleMapper.toEntity(request).getPriceSlots()
            );
        }
        TurfSchedule saved=turfScheduleRepository.save(schedule);
        log.info("Schedule saved for turf {} ",turf.getName());
        return turfScheduleMapper.toResponse(saved,"Turf Schedule saved successfully");

    }
    private void validatePriceSlots(List<TurfScheduleRequest.PriceSlotRequest> priceSlots) {
        for (int i = 0; i < priceSlots.size(); i++) {
            TurfScheduleRequest.PriceSlotRequest a = priceSlots.get(i);
            for (int j = i + 1; j < priceSlots.size(); j++) {
                TurfScheduleRequest.PriceSlotRequest b = priceSlots.get(j);

                // Overlap if ranges intersect
                boolean overlap = !(a.getEndTime().isBefore(b.getStartTime()) ||
                        b.getEndTime().isBefore(a.getStartTime()));

                if (overlap) {
                    throw new CustomException(
                            String.format("Overlapping price slots between %s-%s and %s-%s",
                                    a.getStartTime(), a.getEndTime(),
                                    b.getStartTime(), b.getEndTime()),
                            HttpStatus.BAD_REQUEST
                    );
                }
            }
        }
    }
}
