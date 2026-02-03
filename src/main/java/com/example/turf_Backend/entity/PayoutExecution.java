package com.example.turf_Backend.entity;

import com.example.turf_Backend.enums.ExecutionFailureReason;
import com.example.turf_Backend.enums.ExecutionStatus;
import com.example.turf_Backend.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "payout_execution",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"batch_id","owner_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "batch_id")
    private PayoutBatch batch;

    @OneToMany(mappedBy = "execution",fetch = FetchType.LAZY)
    private List<PayoutBatchItem> items;

    @Column(name = "owner_id",nullable = false)
    private Long ownerId;

    @Column(nullable = false,precision = 19,scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    private Long paidBy;
    private LocalDateTime paidAt;
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code")
    private ExecutionFailureReason failureCode;

    @Column(name="failure_reason",length = 500)
    private String failureReason;
    private  Long failedBy;
    private LocalDateTime failedAt;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "retry_note",length = 500)
    private String retryNote;

    private Long retryBy;
    private LocalDateTime createdAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus reconciliationStatus= ReconciliationStatus.PENDING;

    private LocalDateTime reconciledAt;
    @Column(length = 500)
    private String reconciliationNote;
    //Correction
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correction_of_execution_id")
    private PayoutExecution correctionOf;


}
