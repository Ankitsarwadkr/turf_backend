package com.example.turf_Backend.service;

import com.example.turf_Backend.entity.Payment;
import com.example.turf_Backend.enums.PaymentStatus;
import com.example.turf_Backend.enums.RefundStatus;
import com.example.turf_Backend.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefundService {
    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;

    public void callRazorPayRefund(String paymentId){

        Payment payment=paymentRepository.findById(paymentId).orElseThrow();
        if (payment.getStatus()!= PaymentStatus.SUCCESS){
            log.warn("Payment not successfull, cannot refund: {}",paymentId);
            return;
        }
        if (payment.getPaymentCapturedAt()==null){
            log.warn("Payment not captured, cannot refund :{}",paymentId);
            return;
        }
        if (payment.getRefundAmount()<=0){
            log.warn("Invalid refund amount: {}",paymentId);
            return;
        }
        if (payment.getRazorpayRefundId()!=null){
            log.warn("Refund already initiated: {}",paymentId);
            return;
        }
        if (payment.getRefundStatus()!=RefundStatus.CREATED){
            log.warn("Invalid refund status for processing : {}",payment.getRefundStatus());
            return;
        }
        if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isEmpty()){
            log.error("Missing Razorpay payment Id : {}",paymentId);
            payment.setRefundStatus(RefundStatus.FAILED);
            paymentRepository.save(payment);
        }
        log.info("Debug : payment ID Analysis");

        try{
            JSONObject request=new JSONObject();
            request.put("amount",payment.getRefundAmount()*100); //paise
            request.put("speed","normal");

            log.info("Initiating refund for payment={} razorpayPaymentId={} amount={}",payment.getId(),payment.getRazorpayPaymentId(),payment.getRefundAmount());

            JSONObject refund= razorpayClient.payments
                    .refund(payment.getRazorpayPaymentId(),request)
                    .toJson();

            String refundId=refund.getString("id");
            String refundStatus=refund.optString("status","unknown");
            payment.setRazorpayRefundId(refundId);

            //update
            if ("processed".equalsIgnoreCase(refundStatus)){
                payment.setRefundStatus(RefundStatus.PROCESSED);
                payment.setStatus(PaymentStatus.REFUNDED);
            }else {
                payment.setRefundStatus(RefundStatus.PROCESSING);
            }
            paymentRepository.save(payment);

            log.info("Refund initiated payment={} refundId={} amount={} status={}",payment.getId(),refundId,payment.getRefundAmount(),refundStatus);
        }catch (Exception ex){
            payment.setRefundStatus(RefundStatus.FAILED);
            paymentRepository.save(payment);
            log.error("Refund permanently failed paymentId={} razorpayPaymentId= {} reason={}",payment.getId(),payment.getRazorpayPaymentId(),ex.getMessage(),ex);
        }
    }

    @Transactional
    public List<String> refundsForProcessing() {
        List<Payment> payments=paymentRepository
                .findRefundsForProcessing(RefundStatus.REQUESTED, PageRequest.of(0,5));

        List<String> ids=new ArrayList<>();

        for (Payment p:payments){
            if (p.getRefundStatus()!=RefundStatus.REQUESTED){
             continue;
            }
            p.setRefundStatus(RefundStatus.CREATED);
            ids.add(p.getId());
        }
        if (!ids.isEmpty()){
            paymentRepository.saveAll(payments);
            log.info("Marked {} payments for refund processing ",ids.size());
        }
        return ids;
    }
}
