package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.AddBankRequest;
import com.example.turf_Backend.dto.response.FundAccountResponse;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.service.OwnerBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owner/bank")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerBankController {
    private final OwnerBankService ownerBankService;

    @PostMapping("/setup")
    public ResponseEntity<FundAccountResponse> setUp(@RequestBody AddBankRequest request) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        return ResponseEntity.ok(ownerBankService.setupFundAccount(owner, request));
    }
}
