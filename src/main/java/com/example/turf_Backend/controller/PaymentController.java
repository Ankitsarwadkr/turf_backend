package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.VerifyPaymentRequest;
import com.example.turf_Backend.dto.response.PaymentOderResponse;
import com.example.turf_Backend.dto.response.VerifyPaymentResponse;
import com.example.turf_Backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/order/{bookingId}")
    public ResponseEntity<PaymentOderResponse> createOrder(@PathVariable String bookingId)
    {
        return ResponseEntity.ok(paymentService.createOrder(bookingId));
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyPaymentResponse> verifyPayment(@RequestBody VerifyPaymentRequest request)
    {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }
}
