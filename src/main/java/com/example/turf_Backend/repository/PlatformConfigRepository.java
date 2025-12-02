package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.PlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, Long> {
}