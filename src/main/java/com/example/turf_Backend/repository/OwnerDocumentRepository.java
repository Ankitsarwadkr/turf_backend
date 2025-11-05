package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.OwnerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface OwnerDocumentRepository extends JpaRepository<OwnerDocument, Long> {

    List<OwnerDocument> findByOwnerId(Long ownerId);
}