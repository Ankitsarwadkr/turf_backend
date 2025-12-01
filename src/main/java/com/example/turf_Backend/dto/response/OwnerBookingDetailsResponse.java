package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OwnerBookingDetailsResponse {
    private String bookingId;
    private TurfInfo turf;
    private CustomerInfo customer;
    private Integer amount;
    private String status;
    private LocalDateTime createdAt;
    private List<SlotInfo> slots;

    @Data
    @Builder
    public static class TurfInfo{
        private Long id;
        private String name;
        private String city;
        private String address;
        private String image;
    }
    @Data
    @Builder
    public static class CustomerInfo{
        private String name;
        private String email;
    }
    @Data
    @Builder
    public static  class SlotInfo{
        private String date;
        private String startTime;
        private String endTime;
        private Integer price;
    }


}
