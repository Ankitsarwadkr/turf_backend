package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.DtosProjection.BatchVerificationRowProjection;
import com.example.turf_Backend.dto.DtosProjection.PayoutExecutionDetailsProjection;
import com.example.turf_Backend.dto.DtosProjection.PayoutExecutionProjectionDto;
import com.example.turf_Backend.dto.response.*;
import com.example.turf_Backend.entity.*;
import com.example.turf_Backend.enums.*;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.PayoutMapper;
import com.example.turf_Backend.repository.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayoutService {
    private final OwnerEarningRepository ownerEarningRepository;
    private final PayoutBatchRepository payoutBatchRepository;
    private final PayoutBatchItemRepository payoutBatchItemRepository;
    private final PayoutMapper payoutMapper;
    private final PayoutExecutionRepository payoutExecutionRepository;
    private final PayoutExecutionFailureRepository payoutExecutionFailureRepository;
    private final BookingLedgerRepository bookingLedgerRepository;
    private final PlatformLedgerRepository platformLedgerRepository;


    @Value("${payout.execution.max}")
    private int maxRetryLimit;

    @Transactional
    public PayoutBatchResponse createWeeklyBatch(LocalDate weekStart, LocalDate weekEnd) {
        LocalDateTime startDt = weekStart.atStartOfDay();
        LocalDateTime endDt = weekEnd.atTime(23, 59, 59);


        //Idempontancy check If batch already exists, return the existing one
        if (payoutBatchRepository.existsByWeekStartAndWeekEnd(weekStart,weekEnd))
        {
            log.warn("Batch already exists for week {}- {}. Returning existing batch",weekStart,weekEnd);
            PayoutBatch existing =payoutBatchRepository.findByWeekStartAndWeekEnd(weekStart,weekEnd);
            return payoutMapper.toResponse(existing);
        }
        //Ensure no earnings in this week are already assigned to a batch
        long alreadyAssigned=ownerEarningRepository.countByWeekRangeAndBatchNotNull(startDt,endDt);
        if (alreadyAssigned>0)
        {
            String msg="Some earnings in the week are already assigned to a batch. Count="+alreadyAssigned;

            log.warn(msg);
            throw  new CustomException(msg,HttpStatus.CONFLICT);
        }

        List<OwnerEarning> earnings = ownerEarningRepository.findEligibleForWeeklyBatch(startDt, endDt);
        if (earnings.isEmpty()) {
            throw new CustomException("no earnings eligible for this batch in week " + weekStart + "  -  " + weekEnd, HttpStatus.NOT_FOUND);
        }

        PayoutBatch batch = PayoutBatch.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .type(BatchType.WEEKLY)
                .status(BatchStatus.CREATED)
                .totalAmount(BigDecimal.ZERO)
                .totalOwners(0)
                .build();
        try {
            payoutBatchRepository.save(batch);
        } catch (DataIntegrityViolationException e)
        {
            log.warn("Concurrent batch creataion detected for week {} -{} . Recovering by returning existing batch ",weekStart,weekEnd);
            PayoutBatch existing=payoutBatchRepository.findByWeekStartAndWeekEnd(weekStart,weekEnd);
            if (existing==null){
                throw new CustomException("Failed to create batch and no existing batch found",HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return payoutMapper.toResponse(existing);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        Set<Long> owners = new HashSet<>();
        for (OwnerEarning e : earnings) {
            if (e.getPayoutBatch()!=null)
            {
                log.info("Skipping earning {} because its already assigned to batch {}",e.getId(),e.getPayoutBatch().getId());
                continue;
            }
            PayoutBatchItem item = PayoutBatchItem.builder()
                    .batch(batch)
                    .ownerEarning(e)
                    .amount(e.getAmount())
                    .build();
            payoutBatchItemRepository.save(item);

            e.setPayoutBatch(batch);
            ownerEarningRepository.save(e);
            totalAmount = totalAmount.add(e.getAmount());
            owners.add(e.getOwnerId());
        }
        batch.setTotalAmount(totalAmount);
        batch.setTotalOwners(owners.size());
        payoutBatchRepository.save(batch);
    log.info("Created payout batch {} totalAmount={} ownersCount{} week={} - {}",batch.getId(),totalAmount,owners.size(),weekStart,weekEnd);
        return payoutMapper.toResponse(batch);
    }

    public List<PayoutBatchResponse> getAllBatches() {
        log.info("Fetching all payout batches...");
        List<PayoutBatchResponse> batches=payoutBatchRepository.findAll()
                .stream()
                .map(payoutMapper::toResponse)
                .toList();
        log.info("Found {} payout batches ",batches.size());
        return batches;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PayoutBatchDetailResponse getBatchDetails(Long batchId) {
        log.info("Fetching details for payout batch {}",batchId);

        PayoutBatch batch=payoutBatchRepository.findById(batchId).orElseThrow(()->{
            log.warn("Batch {} not found",batchId);
            return new CustomException("batch not found",HttpStatus.NOT_FOUND);
        });

     List<BatchVerificationRowProjection> rows=payoutBatchItemRepository.findBatchVerificationRows(batchId);
        log.info("Found {} payout items for batch {}",rows.size(),batchId);


      Map<Long,List<OwnerEarningEntry>> ownerMap=new HashMap<>();
      Map<Long,BigDecimal> ownerTotals=new HashMap<>();


        for (BatchVerificationRowProjection r : rows) {

            OwnerEarningEntry entry=OwnerEarningEntry.builder()
                    .earningId(r.getEarningId())
                    .bookingId(r.getBookingId())
                    .turfId(r.getTurfId())
                    .turfName(r.getTurfName())
                    .slotEnd(r.getSlotEnd())
                    .bookingAmount(BigDecimal.valueOf(r.getBookingAmount()))
                    .platformFee(BigDecimal.valueOf(r.getPlatformFee()))
                    .ownerAmount(r.getOwnerAmount())
                    .razorpayOderId(r.getRazorpayOrderId())
                    .razorpayPaymentId(r.getRazorpayPaymentId())
                    .settledAt(r.getSettledAt())
                    .build();

            ownerMap
                    .computeIfAbsent(r.getOwnerId(),k-> new ArrayList<>())
                    .add(entry);
        ownerTotals.merge(r.getOwnerId(),
                r.getOwnerAmount(),
                BigDecimal::add);
        }
        List<OwnerPayoutGroup> ownerGroups=ownerMap.entrySet().stream()
                .map(entry-> OwnerPayoutGroup.builder()
                        .ownerId(entry.getKey())
                        .ownerName(
                                rows.stream()
                                        .filter(r->
                                                r.getOwnerId().equals(entry.getKey()))
                                        .findFirst()
                                        .orElseThrow().getOwnerName())
                        .totalAmount(ownerTotals.get(entry.getKey()))
                        .earnings(entry.getValue())
                        .build()
                ).toList();

        PayoutBatchDetailResponse response=PayoutBatchDetailResponse.builder()
                .batchId(batch.getId())
                .weekStart(batch.getWeekStart())
                .weekEnd(batch.getWeekEnd())
                .totalAmount(batch.getTotalAmount())
                .totalOwners(batch.getTotalOwners())
                .status(batch.getStatus())
                .createdAt(batch.getCreatedAt())
                .owners(ownerGroups)
                .build();

        log.info("Final batch detail response prepared for batch {} owners={}",batchId,ownerGroups.size());
        return response;
    }

    @org.springframework.transaction.annotation.Transactional
    public String approveBatch(Long batchId) {
        PayoutBatch batch=payoutBatchRepository.findById(batchId).orElseThrow(
                ()->new CustomException("Batch not Found",HttpStatus.NOT_FOUND)
        );
        if (batch.getStatus()!=BatchStatus.CREATED)
        {
            throw  new CustomException("Only Created batches can be approved",HttpStatus.CONFLICT);

        }
        batch.setStatus(BatchStatus.APPROVED);
        payoutBatchRepository.save(batch);
        log.info("Batch Approved batchId={}",batchId);
        return "Batch Approved";
    }

    @org.springframework.transaction.annotation.Transactional
    public BatchProcessingResponse startProcessing(Long batchId) {
        log.info("Starting processing for batch {}", batchId);
        PayoutBatch batch = payoutBatchRepository.findById(batchId).orElseThrow(() -> new CustomException("Batch Not Found", HttpStatus.NOT_FOUND));

        int updated = payoutBatchRepository.markProcessing(batchId);
        if (updated == 0) {
            BatchStatus status = payoutBatchRepository.findStatusById(batchId);
            log.info("Batch {} not eligible for. Current status={}", batchId, status);
            return BatchProcessingResponse.builder()
                    .batchId(batchId)
                    .status(status)
                    .executionCount(0)
                    .build();
        }
        log.info("Batch {} moved to PROCESSING (type={})", batchId, batch.getType());
        return switch (batch.getType()) {
            case WEEKLY -> processWeeklyBatch(batch.getId());
            case CORRECTION -> processCorrectionBatch(batch.getId());
        };
    }
    private BatchProcessingResponse processWeeklyBatch(Long batchId) {

        log.info("Processing WEEKLY batch {}", batchId);

        List<OwnerEarning> earnings =
                ownerEarningRepository.findByPayoutBatchId(batchId);

        Map<Long, BigDecimal> ownerTotals =
                earnings.stream()
                        .collect(Collectors.groupingBy(
                                OwnerEarning::getOwnerId,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        OwnerEarning::getAmount,
                                        BigDecimal::add
                                )
                        ));

        int created = 0;
        for (var entry : ownerTotals.entrySet()) {

            Long ownerId = entry.getKey();
            BigDecimal amount = entry.getValue();

            if (payoutExecutionRepository.existsByBatchIdAndOwnerId(
                   batchId, ownerId)) {

                log.warn(
                        "Execution already exists for WEEKLY batch={} owner={}",
                        batchId,
                        ownerId
                );
                continue;
            }
            payoutExecutionRepository.save(
                    PayoutExecution.builder()
                            .batch(payoutBatchRepository.getReferenceById(batchId))
                            .ownerId(ownerId)
                            .amount(amount)
                            .status(ExecutionStatus.PENDING)
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            created++;

            log.info(
                    "Weekly execution created batch={} owner={} amount={}",
                    batchId,
                    ownerId,
                    amount
            );
        }
        return BatchProcessingResponse.builder()
                .batchId(batchId)
                .status(BatchStatus.PROCESSING)
                .executionCount(created)
                .build();
    }
    private BatchProcessingResponse processCorrectionBatch(Long batchId) {

        log.info("Processing CORRECTION batch {}", batchId);

        PayoutBatch batch = payoutBatchRepository.getReferenceById(batchId);   // ✔ managed anchor

        if (payoutExecutionRepository.existsByBatchId(batchId)) {
            log.warn("Correction execution already exists for batch {}", batchId);
            return BatchProcessingResponse.builder()
                    .batchId(batchId)
                    .status(BatchStatus.PROCESSING)
                    .executionCount(0)
                    .build();
        }

        PayoutExecution original = payoutExecutionRepository
                .findById(batch.getOriginalExecutionId())
                .orElseThrow(() -> new CustomException(
                        "Original execution not found for correction batch " + batchId,
                        HttpStatus.NOT_FOUND));

        payoutExecutionRepository.save(
                PayoutExecution.builder()
                        .batch(payoutBatchRepository.getReferenceById(batchId))  // ✔ managed anchor
                        .ownerId(original.getOwnerId())
                        .amount(batch.getCorrectedAmount())
                        .status(ExecutionStatus.PENDING)
                        .correctionOf(original)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        log.info("Correction execution created batch={} originalExecution={} amount={}",
                batchId, original.getId(), batch.getCorrectedAmount());

        return BatchProcessingResponse.builder()
                .batchId(batchId)
                .status(BatchStatus.PROCESSING)
                .executionCount(1)
                .build();
    }

        @org.springframework.transaction.annotation.Transactional
    public MarkExecutionPaidResponse markExecutionPaid(Long executionId, Long adminId, @NotBlank String paymentReference) {
        log.info("Marking execution Paid executionId= {} admin ={} ", executionId, adminId);

        PayoutExecution execution = payoutExecutionRepository.findById(executionId)
                .orElseThrow(() -> new CustomException("Execution not found", HttpStatus.NOT_FOUND));
        if (execution.getStatus() == ExecutionStatus.PAID) {
            log.warn("Execution {} already PAID, returing existing state", executionId);
            return
                    MarkExecutionPaidResponse.builder()
                            .executionId(execution.getId())
                            .status(execution.getStatus())
                            .paidBy(execution.getPaidBy())
                            .paidAt(execution.getPaidAt())
                            .paymentReference(execution.getPaymentReference())
                            .build();

        }
        if (execution.getStatus() != ExecutionStatus.PENDING) {
            throw new CustomException("Execution not in pending state", HttpStatus.CONFLICT);
        }
        execution.setStatus(ExecutionStatus.PAID);
        execution.setPaidBy(adminId);
        execution.setPaidAt(LocalDateTime.now());
        execution.setPaymentReference(paymentReference);

        payoutExecutionRepository.save(execution);
        log.info("Execution {} marked PAID successfully",executionId);

      int updated=  ownerEarningRepository.markPaidOut(execution.getBatch().getId(),execution.getOwnerId());
        log.info("Marked {} earnings as paidOut for batch={} owner ={} ",updated,execution.getBatch().getId(),execution.getOwnerId());
            List<PayoutBatchItem> items =
                    payoutBatchItemRepository.findByBatchIdAndOwnerId(
                            execution.getBatch().getId(),
                            execution.getOwnerId()
                    );

            for (PayoutBatchItem item : items) {
                OwnerEarning e = item.getOwnerEarning();

                bookingLedgerRepository.save(
                        BookingLedger.builder()
                                .bookingId(e.getBookingId())
                                .ownerId(e.getOwnerId())
                                .type(LedgerType.DEBIT)
                                .reason(BookingLedgerReason.PAYOUT)
                                .amount(item.getAmount())
                                .referenceType(ReferenceType.EXECUTION)
                                .referenceId(execution.getPaymentReference())
                                .createdAt(execution.getPaidAt())
                                .build()
                );
            }
            platformLedgerRepository.save(PlatformLedger.builder()
                    .type(LedgerType.DEBIT)
                    .reason(PlatformLedgerReason.OWNER_PAYOUT)
                    .amount(execution.getAmount())
                    .ownerId(execution.getOwnerId())
                    .referenceType(ReferenceType.EXECUTION)
                    .referenceId(execution.getPaymentReference())
                    .createdAt(execution.getPaidAt())
                    .build());
        updateBatchStatus(execution.getBatch().getId());
        return MarkExecutionPaidResponse.builder()
                .executionId(execution.getId())
                .status(execution.getStatus())
                .paidBy(execution.getPaidBy())
                .paidAt(execution.getPaidAt())
                .paymentReference(execution.getPaymentReference())
                .build();
    }


    @org.springframework.transaction.annotation.Transactional
    public void updateBatchStatus(Long batchId)
    {
        long pending=payoutExecutionRepository.countByBatchIdAndStatus(batchId,ExecutionStatus.PENDING);

        boolean hasFailed=payoutExecutionRepository.existsByBatchIdAndStatus(batchId,ExecutionStatus.FAILED);

        PayoutBatch batch =payoutBatchRepository.findById(batchId)
                .orElseThrow(()->new CustomException("Batch Not Found",HttpStatus.NOT_FOUND));
        if (hasFailed){
            batch.setStatus(BatchStatus.FAILED);
            log.warn("Batch {} marked Failed ",batchId);
        } else if (pending==0) {
            batch.setStatus(BatchStatus.COMPLETED);
            log.info("Batch {} COMPELETED ", batchId);
        }
        else
        {
            batch.setStatus(BatchStatus.PROCESSING);
            log.info("Batch {} Still Processing",batchId);
        }
        payoutBatchRepository.save(batch);
    }

    @org.springframework.transaction.annotation.Transactional
    public MarkExecutionFailedResponse markExecutionFailed(Long executionId, Long adminId, @NotNull ExecutionFailureReason code, @NotBlank @Size(max = 500) String failureReason) {

    log.warn("Marking execution FAILED executionId= {} code= {}",executionId,code);

    PayoutExecution execution=payoutExecutionRepository.findById(executionId).orElseThrow(()->new CustomException("Execution not found",HttpStatus.NOT_FOUND));

    //idempontency
        if (execution.getStatus()==ExecutionStatus.FAILED){
            log.warn("Execution {} already paid",executionId);
            return payoutMapper.buildFailedResponse(execution);
        }
        if (execution.getStatus()!=ExecutionStatus.PENDING){
            throw  new CustomException("Only PENDING executions can be failed",HttpStatus.CONFLICT);
        }
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setFailureCode(code);
        execution.setFailureReason(failureReason);
        execution.setFailedBy(adminId);
        execution.setFailedAt(LocalDateTime.now());

        payoutExecutionRepository.save(execution);
        log.warn("Execution {} marked FAILED",executionId);


        updateBatchStatus(execution.getBatch().getId());
        List<PayoutBatchItem> items =
                payoutBatchItemRepository.findByBatchIdAndOwnerId(
                        execution.getBatch().getId(),
                        execution.getOwnerId()
                );

        for (PayoutBatchItem item : items) {
            OwnerEarning e = item.getOwnerEarning();

            bookingLedgerRepository.save(
                    BookingLedger.builder()
                            .bookingId(e.getBookingId())
                            .ownerId(e.getOwnerId())
                            .type(LedgerType.DEBIT)
                            .reason(BookingLedgerReason.PAYOUT)
                            .amount(item.getAmount())
                            .referenceType(ReferenceType.EXECUTION)
                            .referenceId(execution.getPaymentReference())
                            .createdAt(execution.getPaidAt())
                            .build()
            );
        }
        return payoutMapper.buildFailedResponse(execution);
    }

    @org.springframework.transaction.annotation.Transactional
    public RetryExecutionResponse retryExecution(Long executionId, Long adminId, @NotBlank String retryNote) {
        log.warn("Retrying executionId= {} admin ={} ",executionId,adminId);

        PayoutExecution execution=payoutExecutionRepository.findById(executionId).orElseThrow(()->new CustomException("Execution Not Found",HttpStatus.NOT_FOUND));

        if (execution.getStatus()!=ExecutionStatus.FAILED){
            throw  new CustomException("Only  Failed executions can be retried",HttpStatus.CONFLICT);
        }
        payoutExecutionFailureRepository.save(
                PayoutExecutionFailure.builder()
                        .execution(execution)
                        .failureCode(execution.getFailureCode())
                        .failureReason(execution.getFailureReason())
                        .failedBy(execution.getFailedBy())
                        .failedAt(execution.getFailedAt())
                        .attemptNumber(execution.getRetryCount()+1)
                        .build()
        );

        int updated=payoutExecutionRepository.retryExecutionAtomic(executionId,retryNote,maxRetryLimit);
        if (updated==0){
            PayoutExecution current=payoutExecutionRepository.findById(executionId).get();
            if (current.getRetryCount()>=maxRetryLimit){
                throw new CustomException("Retry Limit exceeded",HttpStatus.BAD_REQUEST);
            }
            return RetryExecutionResponse.builder()
                    .executionId(current.getId())
                    .status(current.getStatus())
                    .retryCount(current.getRetryCount())
                    .lastRetryAt(current.getLastRetryAt())
                    .build();
        }
        PayoutExecution retried=payoutExecutionRepository.findById(executionId).get();
        log.warn("Execution {} retried safely (retryCount= {} )",executionId,retried.getRetryCount());
        updateBatchStatus(retried.getBatch().getId());

        return RetryExecutionResponse.builder()
                .executionId(retried.getId())
                .status(retried.getStatus())
                .retryCount(retried.getRetryCount())
                .lastRetryAt(retried.getLastRetryAt())
                .build();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<PayoutExecutionResponse> getExecutionByBatch(Long batchId) {
        log.info("Fetching execution for batchId ={}",batchId);

        List<PayoutExecutionProjectionDto> rows=payoutExecutionRepository.findExecutionProjections(batchId);
        log.info("Found {} executions for batchId ={} ",rows.size(),batchId);

        return rows.stream()
                .map(p->new PayoutExecutionResponse(
                        p.executionId(),
                        p.ownerId(),
                        p.ownerName(),
                        p.amount(),
                        p.status(),
                        p.retryCount(),
                        p.createdAt()
                )).toList();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PayoutExecutionDetailResponse getExecutionDetailsById(Long executionId) {
        log.info("Fetching execution detail execution={} ",executionId);

        PayoutExecutionDetailsProjection p=payoutExecutionRepository.findExecutionDetail(executionId).orElseThrow(()->{
            log.warn("Execution not found executionId={}",executionId);
            return new CustomException("Execution not found",HttpStatus.NOT_FOUND);
        });
        log.info("Execution loaded executionId={} status={} retryCount={}",p.executionId(),p.status(),p.retryCount());
        List<PayoutExecutionFailureResponse> history=payoutExecutionFailureRepository.findByExecutionIdOrderByAttemptNumberAsc(executionId)
                .stream()
                .map(f->{
                    log.debug("Failure history | executionId={} | attempt={} | code={}",executionId,f.getAttemptNumber(),f.getFailureCode());
                    return new PayoutExecutionFailureResponse(
                            f.getAttemptNumber(),
                            f.getFailureCode(),
                            f.getFailureReason(),
                            f.getFailedAt(),
                            f.getFailedBy()
                    );
                }).toList();
        log.info("Execution {} has {} failure history records",executionId,history.size());
        LocalDateTime lastActivity=p.paidAt()!=null ? p.paidAt() :
                p.failedAt()!=null ? p.failedAt() :
                        p.lastRetryAt()!=null ? p.lastRetryAt() :
                                p.createdAt();

        return new PayoutExecutionDetailResponse(
                p.executionId(),
                p.batchId(),
                p.ownerId(),
                p.ownerName(),
                p.amount(),
                p.status(),
                p.failureCode(),
                p.failureReason(),
                p.failedAt(),
                p.failedBy(),
                p.retryCount(),
                p.lastRetryAt(),
                p.paidAt(),
                p.paidBy(),
                p.paymentReference(),
                p.createdAt(),
               lastActivity,
                history
        );
    }
}

