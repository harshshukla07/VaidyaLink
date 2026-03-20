package com.vaidyalink.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PatientRegisterRequest {

    @NotBlank(message="Patient name cannot be empty")
    private String name;

    @Email(message="Please provide a valid email address")
    @NotBlank(message="Email is required")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    @NotBlank(message = "Mobile number is required")
    private String mobile;

    private String gender;

    @Min(value=0, message = "Age cannot be negative")
    private Integer age;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}