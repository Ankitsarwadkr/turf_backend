package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.ReconcileExecutionRequest;
import com.example.turf_Backend.dto.response.ReconcileExecutionResponse;
import com.example.turf_Backend.entity.PayoutExecution;
import com.example.turf_Backend.enums.ExecutionStatus;
import com.example.turf_Backend.enums.ReconciliationStatus;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.repository.PayoutExecutionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutReconciliationService {

    private final PayoutExecutionRepository payoutExecutionRepository;
    @Transactional
    public ReconcileExecutionResponse reconcileExecution(Long executionId, @Valid ReconcileExecutionRequest request, Long adminId) {
        log.info("Reconciling executionId={} by adminId={}",executionId,adminId);

        PayoutExecution execution=payoutExecutionRepository.findByIdForReconciliation(executionId).orElseThrow(
                ()->new CustomException("Payout execution not found ", HttpStatus.NOT_FOUND));

        if (execution.getStatus()!= ExecutionStatus.PAID){
            throw new CustomException("Only PAID executions can be reconciled",HttpStatus.BAD_REQUEST);
        }
        if (execution.getReconciliationStatus()!= ReconciliationStatus.PENDING){
            throw new CustomException("Execution already reconciled",HttpStatus.BAD_REQUEST);
        }
        execution.setReconciliationStatus(request.getStatus());
        execution.setReconciledAt(LocalDateTime.now());
        execution.setReconciliationNote(request.getNote());

        payoutExecutionRepository.save(execution);
        log.info("Reconciliation completed executionId={} status={}", executionId,request.getStatus());

        return ReconcileExecutionResponse.builder()
                .executionId(execution.getId())
                .executionStatus(execution.getStatus())
                .reconciliationStatus(execution.getReconciliationStatus())
                .reconciledAt(execution.getReconciledAt())
                .note(execution.getReconciliationNote())
                .build();
    }
}
