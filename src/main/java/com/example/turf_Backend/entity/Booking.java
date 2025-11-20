package com.example.turf_Backend.entity;

import com.example.turf_Backend.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    private String id;
    @ManyToOne
    private User customer;
    @ManyToOne
    private Turf turf;
    private int amount;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expireAt;
    @ElementCollection
    private List<Long> slotId;
}
