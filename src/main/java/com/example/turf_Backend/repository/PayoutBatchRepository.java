package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.PayoutBatch;
import com.example.turf_Backend.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PayoutBatchRepository extends JpaRepository<PayoutBatch, Long> {
    boolean existsByWeekStartAndWeekEnd(LocalDate weekStart,LocalDate weekEnd);

    PayoutBatch findByWeekStartAndWeekEnd(LocalDate weekStart,LocalDate weekEnd);

    @Modifying
    @Query("""
            UPDATE PayoutBatch b
            SET b.status= 'PROCESSING'
            WHERE b.id= :batchId AND b.status= 'APPROVED'
            """)
    int markProcessing(@Param("batchId") Long batchId);

    @Query(
            "SELECT b.status FROM PayoutBatch b WHERE b.id= :batchId"
    )
    BatchStatus findStatusById( @Param("batchId") Long batchId);

    @Query(
            "SELECT b.id FROM PayoutBatch b WHERE b.status= :status"
    )
    List<Long> findIdsByStatus(@Param("status")BatchStatus status);

    List<PayoutBatch> findByStatus(BatchStatus status);
}