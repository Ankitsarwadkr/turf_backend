package com.example.turf_Backend.controller;


import com.example.turf_Backend.dto.response.PublicTurfCardResponse;
import com.example.turf_Backend.dto.response.PublicTurfDetailsResponse;
import com.example.turf_Backend.service.PublicTurfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/turfs")
@RequiredArgsConstructor
public class PublicTurfController {

    private  final PublicTurfService publicTurfService;
    @GetMapping
    public ResponseEntity<List<PublicTurfCardResponse>> listTurfs()
    {
        return ResponseEntity.ok(publicTurfService.listAllTurfs());
    }
    @GetMapping("{turfId}")
    public  ResponseEntity<PublicTurfDetailsResponse> getTurfDetails(@PathVariable Long turfId)
    {
        return ResponseEntity.ok(publicTurfService.getTurfDetails(turfId));
    }
}
