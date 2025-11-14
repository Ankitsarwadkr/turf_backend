package com.example.turf_Backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "slots",uniqueConstraints = @UniqueConstraint(columnNames = {"turf_id", "date", "start_time", "end_time"}))
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Slots {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turf_id",nullable = false)
    private Turf turf;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;
    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status;


}