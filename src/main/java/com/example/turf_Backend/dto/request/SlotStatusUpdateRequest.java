package com.example.turf_Backend.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotStatusUpdateRequest {
   private  List<Long> slotIds;
    private String status;

}
