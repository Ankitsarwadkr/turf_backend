package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.TurfPriceSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TurfPriceSlotRepository extends JpaRepository<TurfPriceSlot, Long> {
}