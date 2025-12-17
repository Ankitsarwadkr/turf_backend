package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.PayoutExecutionFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayoutExecutionFailureRepository extends JpaRepository<PayoutExecutionFailure, Long> {
    List<PayoutExecutionFailure> findByExecutionIdOrderByAttemptNumberAsc(Long executionId);
}