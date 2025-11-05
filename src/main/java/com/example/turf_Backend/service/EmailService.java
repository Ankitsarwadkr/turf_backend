package com.example.turf_Backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Async("taskExecutor")
    public void sendOwnerRegistrationEmail(String to, String ownerName) {
        log.info("Starting async email to '{}' on thread: {}", to, Thread.currentThread().getName());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Owner Registration Under Verification");
            message.setText("Hello " + ownerName + "\n\nYour registration is under verification. You will be notified once approved.\n\nThank you.");
            mailSender.send(message);
            log.info("Email successfully sent to '{}' ", to);
        } catch (Exception e) {
            log.error("Failed to send email to '{}': {}", to, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendOwnerDecisionMail(String to, String name, boolean approved, String reason) {
        try {

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            if (approved) {
                message.setSubject("Account Approved");
                message.setText("Hello " + name + ",\n\nYour account has been approved. You can now access your dashboard.\n\nThank you.");
            } else {
                message.setSubject("Account Rejected");
                message.setText("Hello " + name + ",\n\nYour account has been rejected.\nReason: " + reason + "\n\nPlease contact support if needed.");
            }
            mailSender.send(message);
            log.info("Decision email sent to {} (Approved: {})", to, approved);
        } catch (Exception e) {
            log.error("Failed to send decision email to {}: {}", to, e.getMessage());
        }
    }
}
