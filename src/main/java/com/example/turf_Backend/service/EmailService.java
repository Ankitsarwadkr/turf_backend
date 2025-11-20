package com.example.turf_Backend.service;

import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.entity.Slots;
import com.example.turf_Backend.repository.SlotsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final SlotsRepository slotsRepository;
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

    @Async
    public void sendBookingConfirmed(String to, Booking booking)
    {
        String subject ="Booking Confirmed- "+booking.getId();
        String body= """
                 Your booking has been CONFIRMED .
                Booking Id: %s
                Turf: %s
                Amount Paid : %s
                Slots: %s
                
                Thank you for booking!
                """.formatted(booking.getId(),
                booking.getTurf().getName(),
                booking.getAmount(),
                formatSlots(booking));
        send(to,subject,body);
    }
    @Async
    public void sendPaymentFailed(String to,Booking booking)
    {
        String subject="Payment Failed - Booking Cancelled";
        String body= """
                Your payment could not be verified and your booking was cancelled.
                
                Booking  ID: %s
                Turf: %s
                Slots: %s
                
                No money has been deducted.
                """.formatted(booking.getId(),
                booking.getTurf().getName(),
                formatSlots(booking));
        send(to,subject,body);
    }
    @Async
    public void sendBookingExpired(String to,Booking booking)
    {
        String subject="Booking Expired-Payment Not Completed";
        String body= """
                Your booking expired because payment was not compelted in time.
                
                Booking ID: %s
                Turf: %s
                Slots: %s
                
                The Slots are now available again.
                """.formatted(booking.getId(),
                booking.getTurf().getName(),
                formatSlots(booking));
        send(to,subject,body);
    }
    private void send(String to,String subject,String body)
    {
        try
        {
            SimpleMailMessage msg=new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);

            log.info("Email sent to {} - {}",to,subject);
        }
        catch (Exception e)
        {
            log.error("failed to send email to {} - {}",to,e.getMessage());
        }
    }
    private String formatSlots(Booking booking)
    {
        List<Slots> slots=slotsRepository.findAllById(booking.getSlotId());
        DateTimeFormatter dateFmt=DateTimeFormatter.ofPattern("dd MM yyyy");
        DateTimeFormatter timeFmt=DateTimeFormatter.ofPattern("hh:mm a");
        StringBuilder sb=new StringBuilder();
        for (Slots s: slots)
        {
            sb.append(s.getDate().format(dateFmt))
                    .append("-")
                    .append(s.getStartTime().format(timeFmt))
                    .append(" to ")
                    .append(s.getEndTime().format(timeFmt))
                    .append(" \n ");
        }
        return sb.toString().trim();
    }



}
