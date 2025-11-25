package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.Slots;
import com.example.turf_Backend.entity.Turf;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SlotsRepository extends JpaRepository<Slots, Long> {
  boolean existsByTurfAndDateAndStartTimeAndEndTime(Turf turf, LocalDate date, LocalTime startTime, LocalTime endTime);

  List<Slots> findByTurfIdAndDateOrderByStartTime(Long turfId, LocalDate date);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Slots s WHERE s.id IN :ids")
  List<Slots> lockByIdsForUpdate(@Param("ids") List<Long> ids);

  @Query("SELECT s FROM Slots s WHERE s.id IN :ids")
  List<Slots> findAllByIds(List<Long> ids);

 @Query("SELECT COUNT (s)>0 FROM Slots s WHERE s.turf.id=:turfId AND s.status= 'BOOKED' AND s.date>=CURRENT_DATE")
  boolean existsFutureBooking(@Param("turfId") Long turfId);
}