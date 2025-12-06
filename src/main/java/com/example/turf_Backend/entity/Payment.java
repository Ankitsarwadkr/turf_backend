package com.example.turf_Backend.entity;

import com.example.turf_Backend.enums.PaymentStatus;
import com.example.turf_Backend.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @Column(length = 36)
    private  String id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id",nullable = false)
    private Booking booking;

    @Column(name = "razorpay_order_id",nullable = false)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    private LocalDateTime paymentCapturedAt;

    private Integer amountPaid;
    private Integer platformFeePaid;
    private Integer commissionPaid;
    private Integer ownerAmountPaid;
    private Integer gatewayFee;
    private Integer gatewayTax;

    @Enumerated(EnumType.STRING)
    private SettlementStatus settlementStatus;

    private LocalDateTime settledAt;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    //convience factory

    public static Payment newPending(Booking booking,String razorpayOrderId,int amount,String currency)
    {
        return Payment.builder()
                .id(UUID.randomUUID().toString())
                .booking(booking)
                .razorpayOrderId(razorpayOrderId)
                .amount(amount)
                .platformFeePaid(null)
                .commissionPaid(null)
                .ownerAmountPaid(null)
                .amountPaid(null)
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .settlementStatus(SettlementStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

}
