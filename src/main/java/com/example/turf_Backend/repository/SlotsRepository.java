package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.Slots;
import com.example.turf_Backend.entity.Turf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SlotsRepository extends JpaRepository<Slots, Long> {
  boolean existsByTurfAndDateAndStartTimeAndEndTime(Turf turf, LocalDate date, LocalTime startTime, LocalTime endTime);

  List<Slots> findByTurfIdAndDateOrderByStartTime(Long turfId, LocalDate date);
}