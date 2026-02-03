package com.example.turf_Backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_batch_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayoutBatchItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id",nullable = false)
    private PayoutBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_earning_id",nullable = false)
    private OwnerEarning ownerEarning;
    @Column(nullable = false)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id")
    private PayoutExecution execution;

    @Builder.Default
    private LocalDateTime createdAt=LocalDateTime.now();
}
