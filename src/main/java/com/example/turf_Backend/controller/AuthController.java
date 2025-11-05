package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.LoginRequest;
import com.example.turf_Backend.dto.request.RegisterOwnerRequest;
import com.example.turf_Backend.dto.request.RegistercustomerRequest;
import com.example.turf_Backend.dto.response.AuthResponse;
import com.example.turf_Backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        authResponse.setToken(null);
        return ResponseEntity.ok(authResponse);

    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response)
    {
        Cookie cookie=new Cookie("token",null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return  ResponseEntity.ok(Map.of("message","Logout Successfull"));
    }

}
