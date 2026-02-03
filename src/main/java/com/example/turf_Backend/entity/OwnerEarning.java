package com.example.turf_Backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "owner_earnings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerEarning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String bookingId;

    @Column(nullable = false,precision = 19,scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime slotEndDateTime;


    @Builder.Default
    @Column(nullable = false)
    private boolean paidOut=false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_batch_id")
    private PayoutBatch payoutBatch;
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt=LocalDateTime.now();

    private LocalDateTime earnedAt;
}
