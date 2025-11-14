package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.TurfImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TurfImageRepository extends JpaRepository<TurfImage, Long> {
}