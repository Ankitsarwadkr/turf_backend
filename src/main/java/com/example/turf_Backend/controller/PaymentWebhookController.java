package com.example.turf_Backend.controller;

import com.example.turf_Backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/payment/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @Value("${razorpay.secret}")
    private String secret;

    @PostMapping
    public ResponseEntity<String> handleWebhook(HttpServletRequest request,
                                                @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            // -------- 1. Read Raw Body --------
            String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

            log.info("Webhook received: {}", body);

            // -------- 2. Verify Signature --------
            if (!isValidSignature(body, signature)) {
                log.warn("INVALID Razorpay webhook signature");
                return ResponseEntity.status(400).body("invalid signature");
            }

            // -------- 3. Parse JSON --------
            JSONObject json = new JSONObject(body);
            String event = json.optString("event");

            log.info("Webhook event: {}", event);

            // -------- 4. Dispatch Events --------
            switch (event) {

                case "payment.captured" -> handlePaymentCaptured(json);

                case "payment.refunded.created",
                     "payment.refunded.updated" ,
                     "payment.refunded" -> handlePaymentRefunded(json);

                default -> log.info("Unhandled webhook event: {}", event);
            }

            // -------- 5. Always return 200 immediately --------
            return ResponseEntity.ok("ok");

        } catch (Exception ex) {
            log.error("Webhook handling error", ex);
            // Still send 200 → prevents Razorpay retry storm
            return ResponseEntity.ok("ok");
        }
    }


    // ────────────────────────────────
    // SIGNATURE VERIFICATION
    // ────────────────────────────────
    private boolean isValidSignature(String body, String sentSignature) {
        try {
            String expected = hmacSHA256(body, secret);
            return expected.equals(sentSignature);
        } catch (Exception ex) {
            log.error("Webhook signature verification failed", ex);
            return false;
        }
    }

    private String hmacSHA256(String data, String key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKey =
                new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }


    // ────────────────────────────────
    // HANDLERS
    // ────────────────────────────────

    private void handlePaymentCaptured(JSONObject json) {
        try {
            JSONObject entity = json.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String rpPaymentId = entity.getString("id");
            String rpOrderId = entity.optString("order_id");

            log.info("Webhook payment captured: order={} payment={}", rpOrderId, rpPaymentId);

            paymentService.markPaymentCaptured(rpOrderId, rpPaymentId);

        } catch (Exception ex) {
            log.error("Error processing payment.captured webhook", ex);
        }
    }


    private void handlePaymentRefunded(JSONObject json) {
        try {
            JSONObject entity = json.getJSONObject("payload")
                    .getJSONObject("refund")
                    .getJSONObject("entity");

            String rpPaymentId = entity.getString("payment_id");

            log.info("Webhook payment refunded: payment={}", rpPaymentId);

            paymentService.markPaymentRefunded(rpPaymentId);

        } catch (Exception ex) {
            log.error("Error processing payment.refunded webhook", ex);
        }
    }
}