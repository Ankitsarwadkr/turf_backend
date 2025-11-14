package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.TurfSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TurfScheduleRepository extends JpaRepository<TurfSchedule, Long> {
 Optional <TurfSchedule> findByTurfId(Long turfId);
}