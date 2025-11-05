package com.example.turf_Backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDocumentResponse {

    private Long id;
    private String fileName;
    private String filePath;
    private LocalDateTime uploadedAt;
}
