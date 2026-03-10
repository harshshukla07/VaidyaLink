package com.vaidyalink.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable=false)
    private LocalDate appointmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;



}
