package com.example.turf_Backend.entity;

import com.example.turf_Backend.enums.LedgerType;
import com.example.turf_Backend.enums.PlatformLedgerReason;
import com.example.turf_Backend.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private LedgerType type;

    @Enumerated(EnumType.STRING)
    private PlatformLedgerReason reason;

    @Column(precision = 19,scale = 2,nullable = false)
    private BigDecimal amount;

    @Column(name = "booking_id")
    private String bookingId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;
    @Column(nullable = false)
    private String referenceId;

    private LocalDateTime createdAt;
}
