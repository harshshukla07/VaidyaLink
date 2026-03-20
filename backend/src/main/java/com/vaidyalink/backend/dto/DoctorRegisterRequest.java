package com.vaidyalink.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DoctorRegisterRequest {

    @NotBlank(message = "Doctor name cannot be empty")
    private String name;

    @Email(message = "Please Provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Speciality cannot be empty")
    private String speciality;

    @Min(value=0, message = "Experience cannot be negative")
    private Integer experience;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}