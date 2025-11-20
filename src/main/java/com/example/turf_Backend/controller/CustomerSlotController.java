package com.example.turf_Backend.controller;


import com.example.turf_Backend.dto.response.CustomerSlotResponse;
import com.example.turf_Backend.service.CustomerSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customer/turfs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Slf4j
public class CustomerSlotController {

    private final CustomerSlotService customerSlotService;

    @GetMapping("/{turfId}/slots")
    public ResponseEntity<List<CustomerSlotResponse>> getSlots(@PathVariable Long turfId,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date)
    {

        System.out.println("Debug -> date received ="+date);
        return ResponseEntity.ok(customerSlotService.getAvailableSlots(turfId,date));
    }
}
