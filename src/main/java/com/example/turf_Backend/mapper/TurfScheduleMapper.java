package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.request.TurfScheduleRequest;
import com.example.turf_Backend.dto.response.TurfScheduleResponse;
import com.example.turf_Backend.entity.TurfPriceSlot;
import com.example.turf_Backend.entity.TurfSchedule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TurfScheduleMapper {
    public TurfSchedule toEntity(TurfScheduleRequest request)
    {
        TurfSchedule schedule=new TurfSchedule();

        schedule.setOpenTime(request.getOpenTime());
        schedule.setCloseTime(request.getCloseTime());
        schedule.setSlotDurationMinutes(request.getSlotDurationMinutes());
        List<TurfPriceSlot> priceSlots=request.getPriceSlots().stream().map(ps->TurfPriceSlot.builder()
                .stratTime(ps.getStartTime())
                .endTime(ps.getEndTime())
                .pricePerSlot(ps.getPricePerSlot())
                .schedule(schedule).build())
                .collect(Collectors.toList());

        schedule.setPriceSlots(priceSlots);
        return schedule;
    }

    public TurfScheduleResponse toResponse(TurfSchedule schedule,String message)
    {
        return TurfScheduleResponse.builder()
                .turfId(schedule.getTurf().getId())
                .openTime(schedule.getOpenTime())
                .closeTime(schedule.getCloseTime())
                .slotDurationMinutes(schedule.getSlotDurationMinutes())
                .priceSlots(schedule.getPriceSlots().stream().map(p->new TurfScheduleResponse.PriceSlotResponse(

                        p.getStratTime(),
                        p.getEndTime(),
                        p.getPricePerSlot()))
                        .collect(Collectors.toList()))
                .message(message)
                .build();
    }
}
