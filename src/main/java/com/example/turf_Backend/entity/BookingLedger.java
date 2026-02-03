package com.example.turf_Backend.entity;

import com.example.turf_Backend.enums.BookingLedgerReason;
import com.example.turf_Backend.enums.LedgerType;
import com.example.turf_Backend.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_ledger",uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id","owner_id","reason","reference_type","reference_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "booking_id",nullable = false)
    private  String bookingId;

    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingLedgerReason reason;

    @Column(precision = 19,scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    @Column(name = "reference_id",nullable = false)
    private String referenceId;

    @Column(nullable = false)
    private LocalDateTime createdAt;


}
