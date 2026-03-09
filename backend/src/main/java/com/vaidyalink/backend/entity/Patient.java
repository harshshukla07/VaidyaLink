package com.vaidyalink.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message="Patient name cannot be empty")
    @Column(nullable = false)
    private String name;

    @Email(message="Please provide a valid email address")
    @NotEmpty(message="Email is required")
    @Column(unique = true, nullable = false)
    private String email;

    private String mobile;

    private String gender;

    @Min(value=0, message = "Age cannot be negative")
    private Integer age;
}