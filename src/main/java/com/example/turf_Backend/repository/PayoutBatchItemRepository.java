package com.example.turf_Backend.repository;

import com.example.turf_Backend.dto.DtosProjection.BatchVerificationRowProjection;
import com.example.turf_Backend.entity.PayoutBatchItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PayoutBatchItemRepository extends JpaRepository<PayoutBatchItem, Long> {
    List<PayoutBatchItem> findByBatchId(Long batchId);

   @Query("""
          SELECT
          e.ownerId as ownerId,
          u.name as ownerName,
          e.id as earningId,
          e.bookingId as bookingId,
          t.id as turfId,
          t.name as turfName,
          
          b.slotEndDateTime as slotEnd,
          
          i.amount as ownerAmount,
          b.amount as bookingAmount,
          b.platformFee as platformFee,
          
          p.razorpayOrderId as razorpayOrderId,
          p.razorpayPaymentId as razorpayPaymentId,
          p.settledAt as settledAt
          FROM PayoutBatchItem i
          JOIN i.ownerEarning e
          JOIN Booking b  ON b.id=e.bookingId
          JOIN Payment p ON p.booking.id=b.id
          JOIN Turf t ON b.turf=t
          JOIN User u ON t.owner=u
          WHERE i.batch.id= :batchId
          ORDER BY u.id, b.slotEndDateTime ASC
          """)
    List<BatchVerificationRowProjection> findBatchVerificationRows(@Param("batchId") long batchId);

   @Query("""
           SELECT i FROM PayoutBatchItem i
           WHERE i.batch.id= :batchId
           AND i.ownerEarning.ownerId= :ownerId
           """)
    List<PayoutBatchItem> findByBatchIdAndOwnerId(Long id, Long ownerId);
}