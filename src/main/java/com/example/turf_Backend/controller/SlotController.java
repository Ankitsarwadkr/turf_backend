package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.SlotStatusUpdateRequest;
import com.example.turf_Backend.dto.response.SlotResponse;
import com.example.turf_Backend.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/owners/turfs/slots")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class SlotController {

    private  final SlotService slotService;
    @PostMapping("/generate/{turfId}")
    public ResponseEntity<String> generateSlots(@PathVariable Long turfId)
    {
        slotService.generateSlots(turfId,7);
        return  ResponseEntity.ok("Slots generated successfully for next 7 days");
    }
    @GetMapping
    public ResponseEntity<List<SlotResponse>> getSlots(@RequestParam Long turfId,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date)
    {
        return ResponseEntity.ok(slotService.getSlotsForTurf(turfId,date));
    }
    @PatchMapping("/status/{turfId}")
    public ResponseEntity<String> updateSlotStatus(@PathVariable Long turfId,
                                                   @RequestBody SlotStatusUpdateRequest request)
    {
        slotService.updateSlotStatus(turfId,request);
        return ResponseEntity.ok("Slot Status updated successfully");
    }

}
