package com.example.turf_Backend.repository;

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
}