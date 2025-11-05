package com.example.turf_Backend.mapper;

import com.example.turf_Backend.dto.response.OwnerDocumentResponse;
import com.example.turf_Backend.dto.response.OwnerResponse;
import com.example.turf_Backend.entity.OwnerDocument;
import com.example.turf_Backend.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AdminMapper {
    public OwnerResponse toOwnerResponse(User owner)
    {
        return  OwnerResponse.builder()
                .id(owner.getId())
                .name(owner.getName())
                .email(owner.getEmail())
                .subscriptionAmount(owner.getSubscriptionAmount())
                .subscriptionStatus(owner.getSubscriptionStatus())
                .createdAt(owner.getCreatedAt())
                .documents(mapDocuments(owner.getDocuments()))
                .build();
    }
public List<OwnerDocumentResponse> mapDocuments(List<OwnerDocument> documents)
{
    if(documents==null)
        return List.of();

    return documents.stream()
            .map(doc->OwnerDocumentResponse.builder()
                    .id(doc.getId())
                    .fileName(doc.getFileName())
                    .filePath(doc.getFilePath())
                    .uploadedAt(doc.getUploadedAt())
                    .build())
            .collect(Collectors.toList());
}
}
