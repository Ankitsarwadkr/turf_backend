package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.CreateCorrectionBatchRequest;
import com.example.turf_Backend.dto.response.CorrectionBatchResponse;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.service.CorrectionBatchService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CorrectionBatchController {
    private final CorrectionBatchService service;

    @PostMapping("/corrections")
    public ResponseEntity<CorrectionBatchResponse> create(@RequestBody  CreateCorrectionBatchRequest request, @AuthenticationPrincipal User admin){
        return ResponseEntity.ok(service.createCorrection(request,admin.getId()));
    }

}
