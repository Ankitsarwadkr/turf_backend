package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.MarkExecutionFailedRequest;
import com.example.turf_Backend.dto.request.MarkExecutionPaidRequest;
import com.example.turf_Backend.dto.request.RetryExecutionRequest;
import com.example.turf_Backend.dto.response.*;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPayoutController {
    private final PayoutService payoutService;


    @PostMapping("/create-weekly")
    public ResponseEntity<PayoutBatchResponse> createWeekly()
    {
        LocalDate weekStart= LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate weekEnd=LocalDate.now().minusWeeks(1).with(DayOfWeek.SUNDAY);

        return ResponseEntity.ok(payoutService.createWeeklyBatch(weekStart,weekEnd));
    }
    @GetMapping("/batches")
    public ResponseEntity<List<PayoutBatchResponse>> getAllBatches()
    {
        return ResponseEntity.ok(payoutService.getAllBatches());
    }
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<PayoutBatchDetailResponse> getBatchDetails(@PathVariable Long batchId)
    {
        return ResponseEntity.ok(payoutService.getBatchDetails(batchId));
    }
    @PostMapping("/batches/{batchId}/approve")
    public ResponseEntity<String> approveBatch(@PathVariable Long batchId)
    {
        return ResponseEntity.ok(payoutService.approveBatch(batchId));
    }
    @PostMapping("/executions/{executionId}/paid")
    public ResponseEntity<MarkExecutionPaidResponse> markExecutionPaid(
            @PathVariable Long executionId,
            @RequestBody @Valid MarkExecutionPaidRequest request,
            @AuthenticationPrincipal User admin
            ){
        return ResponseEntity.ok(payoutService.markExecutionPaid(executionId,admin.getId(),request.getPaymentReference()));
    }
    @PostMapping("/executions/{executionId}/failed")
    public ResponseEntity<MarkExecutionFailedResponse> markFailed(
            @PathVariable Long executionId,
            @Valid @RequestBody MarkExecutionFailedRequest  request,
            @AuthenticationPrincipal User admin)
    {
        return ResponseEntity.ok(payoutService.markExecutionFailed(executionId,admin.getId(),request.getFailureCode(),request.getFailureReason()));
    }

    @PostMapping("/executions/{executionId}/retry")
    public ResponseEntity<RetryExecutionResponse> retryExecution(
            @PathVariable Long executionId,
            @Valid @RequestBody RetryExecutionRequest request,
            @AuthenticationPrincipal User admin
            )
    {
        return ResponseEntity.ok(
                payoutService.retryExecution(
                        executionId,admin.getId(),
                        request.getNote()
                ));
    }
    @GetMapping("/batches/{batchId}/executions")
    public ResponseEntity<List<PayoutExecutionResponse>> getExecutionByBatch(@PathVariable Long batchId)
    {
        return ResponseEntity.ok(payoutService.getExecutionByBatch(batchId));
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<PayoutExecutionDetailResponse> getExecutionDetailsById(@PathVariable Long executionId)
    {
        return ResponseEntity.ok(payoutService.getExecutionDetailsById(executionId));
    }

}
