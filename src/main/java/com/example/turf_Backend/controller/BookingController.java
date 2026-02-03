package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.BookingRequest;
import com.example.turf_Backend.dto.response.*;
import com.example.turf_Backend.service.BookingCancellationService;
import com.example.turf_Backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class BookingController {

    private final BookingService bookingService;
    private final BookingCancellationService cancellationService;

    @PostMapping("/create")
    public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request)
    {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<CustomerBookingListItem>> getMyBookings()
    {
        return ResponseEntity.ok(bookingService.getMyBookings());
    }
    @GetMapping("/{bookingId}")
    public ResponseEntity<CustomerBookingDetails> getBookingDetails(@PathVariable String bookingId)
    {
        return ResponseEntity.ok(bookingService.getBookingDetails(bookingId));
    }
    @GetMapping("/{bookingId}/status")
    public ResponseEntity<BookingStatusResponse> getBookingStatus(@PathVariable String bookingId)
    {
        return ResponseEntity.ok(bookingService.getBookingStatus(bookingId));
    }
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<CancelBookingResponse> cancelByCustomer(@PathVariable String bookingId){
        return ResponseEntity.ok(cancellationService.cancelByCustomer(bookingId));
    }
}
