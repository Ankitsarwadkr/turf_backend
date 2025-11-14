package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.TurfScheduleRequest;
import com.example.turf_Backend.dto.response.TurfScheduleResponse;
import com.example.turf_Backend.service.TurfScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owners/turfs/{turfId}/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class TurfScheduleController {

    private final TurfScheduleService turfScheduleService;

    @PostMapping
    public ResponseEntity<TurfScheduleResponse> createOrUpdateSchedule(@PathVariable Long turfId, @RequestBody TurfScheduleRequest request)
    {
        return ResponseEntity.ok(turfScheduleService.createOrUpdateSchedule(turfId,request));
    }

}
