package com.example.turf_Backend.entity;

import com.example.turf_Backend.enums.ExecutionFailureReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payout_execution_failure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutExecutionFailure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "execution_id")
    private PayoutExecution execution;

    @Enumerated(EnumType.STRING)
    private ExecutionFailureReason failureCode;
    @Column(length = 500)
    private String failureReason;

    private Long failedBy;
    private LocalDateTime failedAt;
    private int attemptNumber;


}

