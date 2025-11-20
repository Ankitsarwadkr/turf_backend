package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.response.PaymentOderResponse;
import com.example.turf_Backend.dto.response.VerifyPaymentResponse;
import com.example.turf_Backend.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentOderResponse toOrderResponse(Payment payment)
    {
        return PaymentOderResponse.builder()
                .bookingId(payment.getBooking().getId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .paymentId(payment.getRazorpayPaymentId())
                .build();
    }

    public VerifyPaymentResponse toVerifyResponse(Payment payment, boolean success)
    {
        return VerifyPaymentResponse.builder()
                .bookingId(payment.getBooking().getId())
                .paymentStatus(success ? "SUCCESS" : "FAILED")
                .message(success ? "Payment verified successfully":"Payment verification failed")
                .paymentId(payment.getRazorpayPaymentId())
                .build();
    }


}
