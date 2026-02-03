package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.response.OwnerBalanceResponse;
import com.example.turf_Backend.dto.response.OwnerNextPayoutRow;
import com.example.turf_Backend.dto.response.OwnerPaidHistoryRow;
import com.example.turf_Backend.dto.response.OwnerWeeklyLedgerResponse;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.service.OwnerEarningsService;
import com.example.turf_Backend.service.OwnerLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/owner/ledger")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerLedgerController {
    private final OwnerLedgerService ownerLedgerService;

    @GetMapping("/balance")
    public ResponseEntity<OwnerBalanceResponse> balance(@AuthenticationPrincipal User owner){
        return  ResponseEntity.ok(ownerLedgerService.getBalance(owner.getId()));
    }
    @GetMapping("/pending")
    public ResponseEntity<List<OwnerNextPayoutRow>> getNextPayout(@AuthenticationPrincipal User owner)
    {
        return ResponseEntity.ok(ownerLedgerService.getNextPayout(owner.getId()));
    }
    @GetMapping("/history")
    public ResponseEntity<List<OwnerPaidHistoryRow>> getHistory(@AuthenticationPrincipal User owner){
        return ResponseEntity.ok(ownerLedgerService.getPaidHistory(owner.getId()));
    }

    @GetMapping("/weekly-ledger")
    public ResponseEntity<OwnerWeeklyLedgerResponse> ledger(
            @AuthenticationPrincipal User owner,
            @RequestParam LocalDate start,
            @RequestParam LocalDate end
            )
    {
        return ResponseEntity.ok(ownerLedgerService.getWeeklyLedger(owner.getId(),start,end));
    }


}
