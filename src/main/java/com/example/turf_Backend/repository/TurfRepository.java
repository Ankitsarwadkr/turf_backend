package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.Turf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurfRepository extends JpaRepository<Turf, Long> {
    List<Turf> findByOwnerId(Long id);
}