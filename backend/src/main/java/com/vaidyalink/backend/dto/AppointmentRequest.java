package com.vaidyalink.backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentRequest {
    private Long patientId;
    private Long doctorId;
    @FutureOrPresent(message = "Appointment date must be today or in future")
    private LocalDate appointmentDate;
    @NotNull(message = "Appointment time cannot be blank")
    private LocalTime appointmentTime;
}
