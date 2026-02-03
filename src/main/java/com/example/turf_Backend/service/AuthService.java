package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.LoginRequest;
import com.example.turf_Backend.dto.request.RegisterOwnerRequest;
import com.example.turf_Backend.dto.request.RegistercustomerRequest;
import com.example.turf_Backend.dto.request.ResetPasswordRequest;
import com.example.turf_Backend.dto.response.AuthResponse;
import com.example.turf_Backend.entity.OwnerDocument;
import com.example.turf_Backend.entity.PasswordResetToken;
import com.example.turf_Backend.enums.Role;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.AuthMapper;
import com.example.turf_Backend.repository.OwnerDocumentRepository;
import com.example.turf_Backend.repository.PasswordResetTokenRepository;
import com.example.turf_Backend.repository.UserRepository;
import com.example.turf_Backend.util.JWTUtil;
import com.example.turf_Backend.util.ResetTokenUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final OwnerDocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final AuthMapper authMapper;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final ImageStorageService imageStorageService;
    private final PasswordResetTokenRepository tokenRepository;

    @Value("${frontend.baseUrl}")
    private String frontendBaseUrl;

    public String registerCustomer(@Valid RegistercustomerRequest request) {
     if (userRepository.existsByEmail(request.getEmail())) throw new CustomException("Email already exists", HttpStatus.CONFLICT);
     User user= authMapper.toUserEntity(request);
     user.setPassword(passwordEncoder.encode(request.getPassword()));
      userRepository.save(user);
     return "Customer registration successful. Please login to continue:";

    }

        @Transactional
    public String registerOwner(@Valid RegisterOwnerRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) throw new CustomException("Email already exists",HttpStatus.CONFLICT);


        User user= authMapper.toOwnerEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
            log.info("Attempting to register new owner: {}", request.getEmail());
            userRepository.save(user);
            log.info("Owner account created with ID: {}", user.getId());



        String filePath= imageStorageService.saveOwnerDocument(request.getDocument(), user.getId());
            OwnerDocument doc = new OwnerDocument();
            doc.setOwner(user);
            doc.setFileName(request.getDocument().getOriginalFilename());
            doc.setFilePath(filePath);
            doc.setDocType(request.getDocType());
            doc.setUploadedAt(LocalDateTime.now());
            documentRepository.save(doc);
            log.info("Document entry persisted for owner ID: {}", user.getId());
        emailService.sendOwnerRegistrationEmail(user.getEmail(),user.getName());
            log.info("Owner registration email queued for: {}", user.getEmail());
        return  "Owner registration successful. Your account is pending for approval : ";
    }


    public AuthResponse login(@Valid LoginRequest request) {
        Authentication authentication=authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword())
        );
        User user= (User) authentication.getPrincipal();
        log.info("User authenticated: {} with role {}", user.getEmail(), user.getRole());
        if (user.getRole() == Role.OWNER) {
            switch (user.getSubscriptionStatus()) {
                case PENDING -> throw new CustomException("Your account is still under verification.", HttpStatus.FORBIDDEN);
                case REJECTED -> throw new CustomException("Your registration was rejected. Please contact support.", HttpStatus.FORBIDDEN);
            }
        }
        String token= jwtUtil.generateToken(user);
        log.info("User '{}' logged in successfully as {}", user.getEmail(), user.getRole());
        return new AuthResponse(token,user.getRole().name(),"Login successful");
    }
    @org.springframework.transaction.annotation.Transactional
    public void initiatePasswordReset(String email) {

        Optional<User> userOpt=userRepository.findByEmail(email);
        if (userOpt.isEmpty())return;

        User user=userOpt.get();
        tokenRepository.deleteAllByUser(user);

        String rawToken= ResetTokenUtil.generateRawToken();

        String tokenHash=ResetTokenUtil.hash(rawToken);

        PasswordResetToken token=new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);

        token.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(token);

        String resetLink=frontendBaseUrl+"/reset-password?token="+rawToken;

        emailService.sendPasswordReset(user.getEmail(),resetLink);

        log.info("RESET LINK {} ",resetLink);


    }

    @org.springframework.transaction.annotation.Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String tokenHash=ResetTokenUtil.hash(req.token());

        PasswordResetToken token=tokenRepository.findByTokenHashAndUsedFalse(tokenHash).orElseThrow(()->new CustomException("Invalid or expired token",HttpStatus.BAD_REQUEST));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new CustomException("Token Expired",HttpStatus.BAD_REQUEST);
        }
        User user=token.getUser();

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        token.setUsed(true);
        tokenRepository.save(token);
    }
}
