package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OwnerBookingListItem {
    private String bookingId;
    private String turfName;
    private String customerName;
    private String customerEmail;
    private Integer amount;
    private String status;
    private LocalDateTime createdAt;
    private List<OwnerSlotItem> slots;
}
