package com.example.turf_Backend.entity;

import com.example.turf_Backend.enums.BatchStatus;
import com.example.turf_Backend.enums.BatchType;
import jakarta.persistence.*;
import lombok.*;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate weekStart;
    private LocalDate weekEnd;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;
    @Enumerated(EnumType.STRING)

    @Column(nullable = false)
    private BatchType type; //WEEKLY // CORRECTION
    @Builder.Default
    @Column(nullable = false)
    private BigDecimal totalAmount=BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false)
    private Integer totalOwners=0;
    @Builder.Default
    private LocalDateTime createdAt=LocalDateTime.now();

    //Correction

    @Column(precision = 19,scale = 2)
    private BigDecimal correctedAmount;
    @Column(name = "original_execution_id",nullable = false)
    private Long originalExecutionId;
    private String reason;



    public static PayoutBatch ref(Long id)
    {
        PayoutBatch b=new PayoutBatch();
        b.setId(id);
        return  b;
    }

}
