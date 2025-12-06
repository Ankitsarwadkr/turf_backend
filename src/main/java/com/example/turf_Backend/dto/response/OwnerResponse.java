package com.example.turf_Backend.dto.response;


import com.example.turf_Backend.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerResponse {
    private Long id;
    private String name;
    private String email;
    private String mobileNo;
    private Double subscriptionAmount;
    private Status subscriptionStatus;
    private LocalDateTime createdAt;
    private List<OwnerDocumentResponse> documents;
}
