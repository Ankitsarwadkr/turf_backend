package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.CreateCorrectionBatchRequest;
import com.example.turf_Backend.dto.response.CorrectionBatchResponse;
import com.example.turf_Backend.entity.PayoutBatch;
import com.example.turf_Backend.entity.PayoutExecution;
import com.example.turf_Backend.enums.BatchStatus;
import com.example.turf_Backend.enums.BatchType;
import com.example.turf_Backend.enums.ExecutionStatus;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.repository.PayoutBatchRepository;
import com.example.turf_Backend.repository.PayoutExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrectionBatchService {
    private final PayoutExecutionRepository executionRepository;
    private final PayoutBatchRepository batchRepository;

    @Transactional
    public CorrectionBatchResponse createCorrection(CreateCorrectionBatchRequest request, Long adminId) {
        log.info("Creating correction for executionId={} by adminId={}",request.executionId(),adminId);

        PayoutExecution original=executionRepository.findForCorrection(request.executionId()).orElseThrow(()->new CustomException("Execution not found", HttpStatus.NOT_FOUND));

        if (original.getStatus()!= ExecutionStatus.PAID){
            throw  new CustomException("Only PAID executions can be corrected",HttpStatus.BAD_REQUEST);
        }
        if (executionRepository.existsByCorrectionOf(original)){
            throw new CustomException("Execution already corrected",HttpStatus.BAD_REQUEST);
        }
        if (request.correctedAmount().compareTo(BigDecimal.ZERO)==0) {
            throw new CustomException("Correction amount cannot be zero",HttpStatus.BAD_REQUEST);
        }

        PayoutBatch batch=new PayoutBatch();
        batch.setType(BatchType.CORRECTION);
        batch.setStatus(BatchStatus.CREATED);
        batch.setReason(request.reason());
        batch.setCorrectedAmount(request.correctedAmount());
        batch=batchRepository.save(batch);
        log.info("Correction batch ={} created for execution {}",batch.getId(),original.getId());
        return new CorrectionBatchResponse(
                batch.getId(),
                original.getId(),
                batch.getCorrectedAmount(),
                batch.getStatus(),
                batch.getReason());
    }
}
