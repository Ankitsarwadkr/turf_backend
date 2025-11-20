package com.example.turf_Backend.repository;

import com.example.turf_Backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

}