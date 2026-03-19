package com.vaidyalink.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.message.Message;

@Entity
@Table(name="doctors")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Doctor name cannot be empty")
    @Column(nullable = false)
    private String name;

    @Email(message = "Please Provide a valid email address")
    @NotBlank(message = "Email is required")
    @Column(unique=true, nullable = false)
    private String email;

    @NotBlank(message = "Speciality cannot be empty")
    @Column(nullable=false)
    private String speciality;

    @Min(value=0, message = "Experience cannot be negative")
    private Integer experience;

    private String password;

}
