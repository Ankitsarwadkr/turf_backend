package com.example.turf_Backend.config;

import com.example.turf_Backend.enums.Role;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Override
    public void run(String... args) throws Exception {
        String adminEmail="admin@gmail.com";
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin already exists: {}", adminEmail);
            return;
        }

        User admin = new User();
        admin.setName("System Admin");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setSubscriptionAmount(null);
        admin.setSubscriptionStatus(null);

        userRepository.save(admin);
        log.info("Default admin created with email: {}", adminEmail);
    }
    }

