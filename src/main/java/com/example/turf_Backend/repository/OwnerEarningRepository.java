package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.OwnerEarning;
import org.apache.catalina.LifecycleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OwnerEarningRepository extends JpaRepository<OwnerEarning, Long> {
    boolean existsByBookingId(String id);

    @Query("""
            SELECT e FROM OwnerEarning e
            WHERE e.slotEndDateTime BETWEEN :start AND :end
            AND e.settled=true
            AND e.paidOut=false
            AND e.payoutBatch IS NULL
            """)
    List<OwnerEarning> findEligibleForWeeklyBatch(@Param("start")LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Query("""
                    SELECT COUNT(e) FROM OwnerEarning e
                    WHERE e.slotEndDateTime
                    BETWEEN :start AND :end
                    AND e.payoutBatch IS NOT NULL
                    """
    )
    long countByWeekRangeAndBatchNotNull(@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    List<OwnerEarning> findByPayoutBatchId(Long batchId);

    @Modifying
    @Query("""
            UPDATE OwnerEarning e
            SET e.paidOut=true
            WHERE e.payoutBatch.id= :batchId
            AND e.ownerId= :ownerId
            """)
    int markPaidOut(@Param("batchId") Long batchId , @Param("ownerId") Long ownerId);

}