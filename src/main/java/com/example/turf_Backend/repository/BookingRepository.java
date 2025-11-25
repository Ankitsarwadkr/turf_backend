package com.example.turf_Backend.repository;

import com.example.turf_Backend.dto.DtosProjection.CustomerBookingDetailsProjection;
import com.example.turf_Backend.dto.DtosProjection.CustomerBookingListProjection;
import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {
    @Query("SELECT b FROM Booking b WHERE b.status='PENDING_PAYMENT' AND b.expireAt<:now")
    List<Booking> findExpired(LocalDateTime now);

    List<Booking> findByStatusAndExpireAtBefore(BookingStatus bookingStatus, LocalDateTime now);

    List<Booking> findByCustomerIdAndStatus(Long customerId, BookingStatus bookingStatus);

    @Query("""
            SELECT
            b.id AS bookingId,
            t.id AS turfId,
            t.name AS turfName,
            t.city AS turfCity,
            b.amount AS amount,
            b.status AS bookingStatus,
            p.status AS paymentStatus,
            p.razorpayPaymentId AS paymentId,
            b.slotId AS slotId
           
            FROM Booking b
            JOIN b.turf t
            LEFT JOIN Payment p ON p.booking.id=b.id
   
            WHERE b.customer.id=:customerId
            ORDER BY b.createdAt DESC
            """)
    List<CustomerBookingListProjection> findAllBookingsForCustomer(Long customerId);

    @Query("""
            SELECT
            b.id AS bookingId,
            t.id AS turfId,
            t.name AS turfName,
            t.city AS turfCity,
            t.address AS turfAddress,
            b.amount AS amount,
            b.status AS bookingStatus,
            p.status AS paymentStatus,
            p.razorpayPaymentId AS paymentId,
            b.createdAt AS createdAt,
            b.expireAt AS expireAt
            
            FROM Booking b
            JOIN b.turf t
            LEFT JOIN Payment p ON p.booking.id=b.id
            WHERE b.id=:bookingId
            AND b.customer.id=:customerId
            """)
    CustomerBookingDetailsProjection findBookingDetails(String bookingId,Long customerId);
}