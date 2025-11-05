package com.example.turf_Backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "owner_documents")
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id",nullable = false)
    private User owner;
    private String fileName;
    private String filePath;
    @CreationTimestamp
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;




}
