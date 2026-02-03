package com.example.turf_Backend.repository;

import com.example.turf_Backend.dto.DtosProjection.NextPayoutProjection;
import com.example.turf_Backend.dto.DtosProjection.PaidHistoryProjection;
import com.example.turf_Backend.entity.BookingLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingLedgerRepository extends JpaRepository<BookingLedger, Long> {

    @Query("""
            SELECT COALESCE(SUM(
            CASE WHEN l.type='CREDIT' THEN l.amount ELSE -l.amount END ),0)
            FROM BookingLedger l
            WHERE l.ownerId= :ownerId
            """)
    BigDecimal getOwnerBalance(Long ownerId);

    @Query("""
            SELECT l.bookingId AS bookingId,
            l.amount AS amount,
            l.createdAt AS createdAt,
            l.reason AS reason
            FROM BookingLedger l
            WHERE l.ownerId=:ownerId
            AND l.reason='OWNER_EARNING'
            AND NOT EXISTS(
            SELECT l FROM BookingLedger p
            WHERE p.reason='PAYOUT'
            AND p.referenceId= l.referenceId
            )
            ORDER BY l.createdAt DESC
            """)
    List<NextPayoutProjection> findPendingOwnerEarnings(Long ownerId);

    @Query("""
            SELECT l.amount AS amount,
            l.createdAt AS createdAt,
            l.referenceId AS referenceId
            FROM BookingLedger l
            WHERE l.ownerId=:ownerId
            AND l.reason='PAYOUT'
            ORDER BY l.createdAt DESC
            """)
    List<PaidHistoryProjection> findOwnerHistory(Long ownerId);

    @Query("""
            SELECT COALESCE(SUM(
            CASE WHEN l.type='CREDIT' THEN l.amount ELSE -l.amount END),0)
            FROM BookingLedger l
            WHERE l.ownerId=:ownerId
            AND l.createdAt < :start
            """)
    BigDecimal getOpeningBalance(Long ownerId, LocalDateTime start);

    @Query("""
            SELECT l FROM BookingLedger l
            WHERE l.ownerId= :ownerId
            AND l.createdAt BETWEEN :start AND :end
            ORDER BY l.createdAt
            """)
    List<BookingLedger> findOwnerLedgerRange(Long ownerId,LocalDateTime start,LocalDateTime end);
}