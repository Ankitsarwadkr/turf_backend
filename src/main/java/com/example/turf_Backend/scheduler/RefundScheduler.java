package com.example.turf_Backend.scheduler;

import com.example.turf_Backend.entity.Payment;
import com.example.turf_Backend.enums.RefundStatus;
import com.example.turf_Backend.repository.PaymentRepository;
import com.example.turf_Backend.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundScheduler {
    private final PaymentRepository paymentRepository;
    private final RefundService refundService;

    @Scheduled(fixedDelay = 10_000)
    public void processRefunds(){
    List<String> paymentsIds=refundService.refundsForProcessing();
    for (String paymentId : paymentsIds){
        try
        {
            refundService.callRazorPayRefund(paymentId);
        }catch (Exception e){
            log.error("Refund processing failed payment={}",paymentId,e);
        }
    }
    }
}