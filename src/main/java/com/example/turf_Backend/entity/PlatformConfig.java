package com.example.turf_Backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformConfig {
    @Id
    private Long id=1L;
    private int platformFee;
    private int commissionPercentage;
}
