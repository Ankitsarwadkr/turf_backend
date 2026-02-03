package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.Payment;
import com.example.turf_Backend.enums.PaymentStatus;
import com.example.turf_Backend.enums.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface PaymentRepository extends JpaRepository<Payment, String> {
Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

Optional<Payment> findFirstByBookingIdAndStatus(String bookingId, PaymentStatus status);

Optional<Payment> findByBookingId(String bookingId);

    Optional<Payment> findByRazorpayPaymentId(String rpPaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.booking.id= :bookingId")
    Optional<Payment> lockByBookingId(String bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Payment p
            where p.refundStatus= :status
            order by p.createdAt asc
            """)
    List<Payment> findRefundsForProcessing(@Param("status")RefundStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id= :id")
    Optional<Payment> lockById(String id);
}