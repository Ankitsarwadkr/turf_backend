package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.UpdateProfileRequest;
import com.example.turf_Backend.dto.response.ProfileResponse;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.enums.Role;
import com.example.turf_Backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(Authentication auth){
        if (auth==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user=(User) auth.getPrincipal();

        ProfileResponse response=new ProfileResponse(
                user.getId(),
                user.getRole().name(),
                user.getName(),
                user.getEmail(),
                user.getMobileNo(),
                user.getRole()== Role.OWNER ? user.getSubscriptionStatus().name():null,
                user.getRole()==Role.OWNER ? user.getSubscriptionAmount() : null
        );
        return ResponseEntity.ok(response);
    }
    @PutMapping("/update")
    public ResponseEntity<ProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest req, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(profileService.updateProfile(user, req));
    }

}
