package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.BookingRequest;
import com.example.turf_Backend.dto.response.BookingResponse;
import com.example.turf_Backend.dto.response.BookingStatusResponse;
import com.example.turf_Backend.dto.response.CustomerBookingDetails;
import com.example.turf_Backend.dto.response.CustomerBookingListItem;
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
}
