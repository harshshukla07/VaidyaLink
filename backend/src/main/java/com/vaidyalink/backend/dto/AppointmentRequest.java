package com.vaidyalink.backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentRequest {
    private Long patientId;
    private Long doctorId;
    @FutureOrPresent(message = "Appointment date must be today or in future")
    private LocalDate appointmentDate;
}
