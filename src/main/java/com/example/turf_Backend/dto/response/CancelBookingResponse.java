package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.BookingStatus;
import com.example.turf_Backend.enums.RefundStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CancelBookingResponse {
    private String bookingId;
    private BookingStatus bookingStatus;
    private RefundStatus refundStatus;
    private String refundMessage;
}
