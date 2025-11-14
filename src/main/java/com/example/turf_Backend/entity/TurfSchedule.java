package com.example.turf_Backend.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "turf_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurfSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turf_id",nullable = false)
    private Turf turf;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    @Column(nullable = false)
    private int slotDurationMinutes;

    @OneToMany(mappedBy = "schedule",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<TurfPriceSlot> priceSlots =new ArrayList<>();

}
