package com.example.turf_Backend.repository;

import com.example.turf_Backend.dto.DtosProjection.CustomerBookingDetailsProjection;
import com.example.turf_Backend.dto.DtosProjection.CustomerBookingListProjection;
import com.example.turf_Backend.dto.DtosProjection.OwnerBookingDetailsProjection;
import com.example.turf_Backend.dto.DtosProjection.OwnerBookingListProjection;
import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
            b.createdAt AS createdAt,
            s.date AS slotDate,
            s.startTime AS slotStartTime,
            s.endTime AS slotEndTime
            FROM Booking b
            JOIN b.turf t
            LEFT JOIN Payment p ON p.booking.id=b.id
            JOIN b.slots s
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
            (SELECT ti.filePath
            FROM TurfImage ti
            WHERE ti.turf.id= t.id
            ORDER BY ti.id ASC
            LIMIT 1) AS turfImage,
            b.amount AS amount,
            b.status AS bookingStatus,
            p.status AS paymentStatus,
            p.razorpayPaymentId AS paymentId,
            b.createdAt AS createdAt,
            b.expireAt AS expireAt,
            s.date AS slotDate,
            s.startTime AS slotStartTime,
            s.endTime AS slotEndTime,
            s.price AS slotPrice
            FROM Booking b
            JOIN b.turf t
            LEFT JOIN Payment p ON p.booking.id=b.id
            JOIN b.slots s
            WHERE b.id=:bookingId
            AND b.customer.id=:customerId
            ORDER BY s.date ASC, s.startTime ASC
            """)
    List<CustomerBookingDetailsProjection> findBookingDetails(String bookingId,Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id= :id")
    Optional<Booking> lockBookingForUpdate(@Param("id") String id);

    @Query("""
            SELECT
            b.id AS bookingId,
            t.id AS turfId,
            t.name AS turfName,
            
            c.name AS customerName,
            c.email AS customerEmail,
            
            b.amount AS amount,
            b.status AS bookingStatus,
            b.createdAt AS createdAt,
            
            s.id AS slotId,
            s.date AS slotDate,
            s.startTime AS slotStartTime,
            s.endTime AS slotEndTime,
            s.price AS slotPrice,
            s.status AS slotStatus
            
            FROM Booking b
            JOIN b.turf t
            JOIN b.customer c
            JOIN b.slots s
            
            WHERE t.owner.id= :ownerId
            ORDER BY b.createdAt DESC, s.date ASC, s.startTime ASC
            """)
    List<OwnerBookingListProjection> findBookingRows(Long ownerId);

    @Query("""
            SELECT
            b.id AS bookingId,
            t.id AS turfId,
            t.name AS turfName,
            t.city AS turfCity,
            t.address AS turfAddress,
            (SELECT ti.filePath
            FROM TurfImage ti 
            WHERE ti.turf.id= t.id
            ORDER BY ti.id ASC
            LIMIT 1) AS TurfImage,
            
            c.name AS customerName,
            c.email AS customerEmail,
            
            b.amount AS amount,
            b.status AS bookingStatus,
            b.createdAt AS createdAt,
            
            s.date AS slotDate,
            s.startTime AS slotStartTime,
            s.endTime AS slotEndTime,
            s.price AS slotPrice
            
            FROM Booking b
            JOIN b.turf t
            JOIN b.customer c
            JOIN b.slots s
            
            WHERE b.id= :bookingId
            AND t.owner.id= :ownerId
            ORDER BY s.date ASC, s.startTime ASC
            """)
    List<OwnerBookingDetailsProjection> findOwnerBookingDetails(String bookingId,Long ownerId);

}