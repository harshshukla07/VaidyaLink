package com.vaidyalink.backend.dto;

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
    private LocalDate appointmentDate;
}
