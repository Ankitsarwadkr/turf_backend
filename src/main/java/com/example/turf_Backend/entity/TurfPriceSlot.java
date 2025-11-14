package com.example.turf_Backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "turf_price_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurfPriceSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turf_schedule_id",nullable = false)
    private TurfSchedule schedule;

    @Column(nullable = false)
    private LocalTime stratTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int pricePerSlot;

}
