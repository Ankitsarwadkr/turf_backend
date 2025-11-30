package com.example.turf_Backend.dto.response;

import com.example.turf_Backend.enums.BookingStatus;
import lombok.Data;

@Data
public class BookingStatusResponse {
  private String bookingId;
  private BookingStatus bookingStatus;
  private long remainingMinutes;
}
