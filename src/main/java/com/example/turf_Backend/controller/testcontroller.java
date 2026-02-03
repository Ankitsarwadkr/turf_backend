package com.example.turf_Backend.controller;

import com.example.turf_Backend.service.PaymentService;
import com.example.turf_Backend.service.PaymentWebhookService;
import lombok.Data;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Data
public class testcontroller {

    private final PaymentWebhookService webhookService;

    @PostMapping("/api/test/settle/{rpPaymentId}")
    public void testSettle(@PathVariable String rpPaymentId)
    {
       webhookService.markPaymentSettled(rpPaymentId, LocalDateTime.now());
    }

}
