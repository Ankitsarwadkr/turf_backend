package com.example.turf_Backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="owner_fund_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerFundAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id",nullable = false,unique = true)
    private User owner;

    private String razorpayContactId;
    private String razorpayFundAccountId;

    private String accountHolderName;
    private String accountNumberMasked;
    private String ifsc;
    private boolean verified;

    private LocalDateTime createdAt;


}
