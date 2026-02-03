package com.example.turf_Backend.mapper;

import com.example.turf_Backend.enums.RefundReason;
import org.springframework.stereotype.Component;


public class RefundMessageMapper {
    public static String toCustomerMessage(RefundReason reason){
        if (reason==null) return "Refund initiated";

        return switch (reason){
            case CUSTOMER_CANCELLED_BEFORE_CUTOFF -> "Refund will be processed in 5-7 business days.";
            case OWNER_CANCELLED_BEFORE_SLOT -> "Owner Cancelled the booking Full refund initiated.";
            case ADMIN_CANCELLED_EMERGENCY -> "Booking Cancelled due to an issue. Full refund initiated.";
            default -> "Refund initiated. Please contact support if needed.";
        };
    }
}
