package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.ReconcileExecutionRequest;
import com.example.turf_Backend.dto.response.ReconcileExecutionResponse;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.service.PayoutReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPayoutReconciliationController {
    private final PayoutReconciliationService payoutReconciliationService;

    @PostMapping("/executions/{executionId}/reconcile")
    public ResponseEntity<ReconcileExecutionResponse> reconcile(@PathVariable Long executionId,
                                                                @Valid @RequestBody
                                                                ReconcileExecutionRequest request, @AuthenticationPrincipal User admin)
    {
        return ResponseEntity.ok(payoutReconciliationService.reconcileExecution(executionId,request,admin.getId()));
    }
}
