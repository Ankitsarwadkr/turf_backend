package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.TurfImage;
import com.example.turf_Backend.mapper.BookingMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TurfImageRepository extends JpaRepository<TurfImage, Long> {
    Optional<TurfImage> findByTurfIdOrderByIdAsc(Long turfId);
}