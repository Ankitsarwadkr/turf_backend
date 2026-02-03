package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.*;
import com.example.turf_Backend.dto.response.AuthResponse;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.enums.Role;
import com.example.turf_Backend.service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/register/customer")
    public ResponseEntity<String> registerCustomer(@Valid @RequestBody RegistercustomerRequest request)
    {
        String msg=authService.registerCustomer(request);
        return new ResponseEntity<>(msg, HttpStatus.CREATED);
    }
    @PostMapping("/register/owner")
    public ResponseEntity<String> registerOwner(@Valid @ModelAttribute RegisterOwnerRequest request)
    {
        String msg= authService.registerOwner(request);
        return new ResponseEntity<>(msg,HttpStatus.CREATED);
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse httpServletResponse)
    {
        AuthResponse authResponse=authService.login(request);
        String jwt=authResponse.getToken();

        Cookie cookie=new Cookie("token",jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);//false for dev stage
        cookie.setPath("/");
        cookie.setMaxAge(24*60*60);
        httpServletResponse.addCookie(cookie);
        return ResponseEntity.ok(authResponse);

    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response)
    {
        Cookie cookie=new Cookie("token",null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return  ResponseEntity.ok(Map.of("message","Logout Successfull"));
    }
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth){
        if (auth==null) return ResponseEntity.status(401).build();

        User user=(User) auth.getPrincipal();

        String status;
        if (user.getRole()== Role.OWNER){
            status=user.getSubscriptionStatus().name();
        }
        else {
            status="ACTIVE";
        }
        return ResponseEntity.ok(Map.of(
                "id",user.getId(),
                "role",user.getRole().name(),
                "status",status
        ));
    }

    @PostMapping("/forget-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req){
        authService.initiatePasswordReset(req.email());
        return ResponseEntity.ok(Map.of("message ","If account exists, a reset link has been sent"));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req)
    {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message","Password Reset Successful"));
    }
}
