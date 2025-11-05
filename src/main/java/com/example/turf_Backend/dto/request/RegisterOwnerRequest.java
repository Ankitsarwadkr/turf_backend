package com.example.turf_Backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class RegisterOwnerRequest {

    @NotBlank
    private String name;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotNull
    private MultipartFile document;
    private Double subscriptionAmount;
}
