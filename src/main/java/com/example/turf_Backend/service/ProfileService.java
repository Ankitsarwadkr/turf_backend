package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.UpdateProfileRequest;
import com.example.turf_Backend.dto.response.ProfileResponse;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;

    @Transactional
    public ProfileResponse updateProfile(User user, @Valid UpdateProfileRequest req) {
        user.setName(req.name());
        user.setMobileNo(req.mobileNo());

        userRepository.save(user);

        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobileNo(),
                user.getRole().name(),
                null
                ,null
        );
    }
}
