package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.response.OwnerBookingListItem;
import com.example.turf_Backend.service.OwnerBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/owner/bookings")
@RequiredArgsConstructor
public class OwnerBookingController {
    private final OwnerBookingService ownerBookingService;

    @GetMapping
    public ResponseEntity<List<OwnerBookingListItem>> getBookings()
    {
        return ResponseEntity.ok(ownerBookingService.getBookingListForOwner());
    }
}
