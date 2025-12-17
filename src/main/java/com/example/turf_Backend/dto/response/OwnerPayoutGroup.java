package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OwnerPayoutGroup {
    private Long ownerId;
    private String ownerName;
    private BigDecimal totalAmount;
    private List<OwnerEarningEntry> earnings;
}
