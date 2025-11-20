package com.example.turf_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "turfs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turf {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private String mapUrl;//view in map (location)
    @Column(nullable = false)
    private  String city;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String amenities;

    @Column(nullable = false)
    private String turfType;

    @Column(nullable = false)
    private boolean available = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "turf", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TurfImage> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToOne(mappedBy = "turf",cascade = CascadeType.ALL,orphanRemoval = true)
    private TurfSchedule schedule;

    @OneToMany(mappedBy = "turf",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Slots> slots=new ArrayList<>();
}