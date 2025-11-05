package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.LoginRequest;
import com.example.turf_Backend.dto.request.RegisterOwnerRequest;
import com.example.turf_Backend.dto.request.RegistercustomerRequest;
import com.example.turf_Backend.dto.response.AuthResponse;
import com.example.turf_Backend.entity.OwnerDocument;
import com.example.turf_Backend.entity.Role;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.AuthMapper;
import com.example.turf_Backend.repository.OwnerDocumentRepository;
import com.example.turf_Backend.repository.UserRepository;
import com.example.turf_Backend.util.JWTUtil;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

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

    @Value("${file.upload-dir}")
    private String fileUploadDir;
 private static final List<String> ALLOWED_FILE_TYPES=List.of("application/pdf","image/png","image/jpeg");
 private static final long MAX_FILE_SIZE=5*1024*1024;

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

        validateDocument(request.getDocument());
        User user= authMapper.toOwnerEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
            log.info("Attempting to register new owner: {}", request.getEmail());
            userRepository.save(user);
            log.info("Owner account created with ID: {}", user.getId());



        String filePath=saveDocument(request.getDocument(),user.getId());
            log.info("Saved document for owner {}: {}", user.getEmail(), filePath);
            OwnerDocument doc = new OwnerDocument();
            doc.setOwner(user);
            doc.setFileName(request.getDocument().getOriginalFilename());
            doc.setFilePath(filePath);
            doc.setUploadedAt(LocalDateTime.now());
            documentRepository.save(doc);
            log.info("Document entry persisted for owner ID: {}", user.getId());
        emailService.sendOwnerRegistrationEmail(user.getEmail(),user.getName());
            log.info("Owner registration email queued for: {}", user.getEmail());
        return  "Owner registration successful. Your account is pending for approval : ";
    }
    private void validateDocument(MultipartFile file)
    {
        if (!ALLOWED_FILE_TYPES.contains(file.getContentType()))
        {
            throw new CustomException("Invalid file type. Only PDF, PNG, JPG are allowed.", HttpStatus.BAD_REQUEST);

        }
        if (file.getSize()>MAX_FILE_SIZE)
        {
            throw new CustomException("File size exceeds the 5MB limit.", HttpStatus.BAD_REQUEST);
        }
        if (file == null || file.isEmpty()) {
            throw new CustomException("Document is required and cannot be empty.", HttpStatus.BAD_REQUEST);
        }

    }
    private String saveDocument(MultipartFile file, Long ownerId) {
        log.info("Uploading document for owner ID {} to path {}", ownerId, fileUploadDir);

        // Get project root dynamically
        String projectDir = System.getProperty("user.dir"); // points to project root
        String folder = projectDir + "/" + fileUploadDir + "/" + ownerId;

        File dir = new File(folder);
        if (!dir.exists()) dir.mkdirs(); // create nested folders if not exist

        // Add timestamp to filename to avoid collisions
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String filePath = folder + "/" + fileName;

        try {
            file.transferTo(Paths.get(filePath).toFile());
        } catch (IOException ex) {
            log.error("Document upload failed for owner {}: {}", ownerId, ex.getMessage(), ex);
            throw new CustomException("Document upload failed: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return filePath;
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
}
