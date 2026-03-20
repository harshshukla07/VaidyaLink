package com.vaidyalink.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email format")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}