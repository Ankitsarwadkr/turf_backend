package com.example.turf_Backend.repository;

import com.example.turf_Backend.dto.DtosProjection.PayoutExecutionDetailsProjection;
import com.example.turf_Backend.dto.DtosProjection.PayoutExecutionProjectionDto;
import com.example.turf_Backend.entity.PayoutExecution;
import com.example.turf_Backend.enums.ExecutionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayoutExecutionRepository extends JpaRepository<PayoutExecution, Long> {


    long countByBatchIdAndStatus(Long batchId, ExecutionStatus status);
    boolean existsByBatchIdAndStatus(Long batchId,ExecutionStatus status);


    boolean existsByBatchIdAndOwnerId(Long batchId, Long ownerId);

    @Modifying(clearAutomatically = true,flushAutomatically = true)
    @Query("""
            UPDATE PayoutExecution e
            SET
                e.status= 'PENDING',
                e.retryCount= e.retryCount+1,
                e.lastRetryAt= CURRENT_TIMESTAMP,
                e.retryNote= :note,
                e.failureCode= NULL,
                e.failureReason= NULL,
                e.failedBy= NULL,
                e.failedAt= NULL
                WHERE
                e.id= :executionId
                AND e.status= 'FAILED'
                AND e.retryCount< :maxRetries
            """)
    int retryExecutionAtomic(@Param("executionId") Long executionId,@Param("note") String note,@Param("maxRetries") int maxRetries);


    @Query("""
            SELECT new com.example.turf_Backend.dto.DtosProjection.PayoutExecutionProjectionDto(
            e.id,
            e.ownerId,
            u.name,
            e.amount,
            e.status,
            e.retryCount,
            e.createdAt
            )
            FROM PayoutExecution e
            JOIN User u ON u.id=e.ownerId
            WHERE e.batch.id= :batchId
            ORDER BY e.createdAt DESC
            """)
    List<PayoutExecutionProjectionDto> findExecutionProjections(@Param("batchId") Long batchId);

    @Query("""
            SELECT new
            com.example.turf_Backend.dto.DtosProjection.PayoutExecutionDetailsProjection(
            e.id,
            e.batch.id,
            e.ownerId,
            u.name,
            e.amount,
            e.status,
            e.failureCode,
            e.failureReason,
            e.failedAt,
            e.failedBy,
            e.retryCount,
            e.lastRetryAt,
            e.paidAt,
            e.paidBy,
            e.paymentReference,
            e.createdAt
            )
            FROM PayoutExecution e
            JOIN User u ON u.id=e.ownerId
            WHERE e.id= :executionId
            """)
    Optional<PayoutExecutionDetailsProjection> findExecutionDetail(@Param("executionId") Long executionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM PayoutExecution e
            WHERE e.id= :executionId
            """)
    Optional<PayoutExecution> findByIdForReconciliation(@Param("executionId") Long executionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM PayoutExecution e WHERE e.id= :id")
    Optional<PayoutExecution> findForCorrection(@Param("id") Long id);

    boolean existsByCorrectionOf(PayoutExecution execution);
    boolean existsByBatchId(Long batchId);


}