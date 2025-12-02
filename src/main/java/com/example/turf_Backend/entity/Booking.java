package com.example.turf_Backend.entity;

import com.example.turf_Backend.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private int amount;// final payable amount by customer
    @Column(nullable = false)
    private int slotTotal;
    @Column(nullable = false)
    private int platformFee;
    @Column(nullable = false)
    private int commissionAmount;
    @Column(nullable = false)
    private int ownerEarning; //payout to owner

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expireAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "booking_slots",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "slot_id")
    )
    private Set<Slots> slots=new HashSet<>();

    //helper method
    public List<Long> getSlotIds()
    {
        return slots.stream()
                .map(Slots::getId)
                .sorted().toList();
    }
}
