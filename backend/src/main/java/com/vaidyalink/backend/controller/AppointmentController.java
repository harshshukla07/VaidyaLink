package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.dto.AppointmentResponse;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import com.vaidyalink.backend.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.service.PatientService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final PatientService patientService;

    public AppointmentController(AppointmentService appointmentService, PatientService patientService) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
    }

    // Helper method to convert raw Entity into a safe DTO
    private AppointmentResponse mapToResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getName(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getStatus()
        );
    }

    private void verifyPatientOwnership(Long requestedPatientId, Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        String loggedInEmail = authentication.getName();

        if (role.equals("ROLE_PATIENT")) {
            Patient patient = patientService.getPatientById(requestedPatientId);
            if (!patient.getEmail().equals(loggedInEmail)) {
                throw new AccessDeniedException("Viewing other patient's appointments is not allowed!");
            }
        }
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/book")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @Valid @RequestBody AppointmentRequest request,
            Authentication authentication){

        // Shield Check
        verifyPatientOwnership(request.getPatientId(), authentication);

        Appointment appointment = appointmentService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(appointment));
    }

    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<AppointmentResponse>> getPatientAppointments(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        // Shield Check
        verifyPatientOwnership(patientId, authentication);

        Page<Appointment> appointments = appointmentService.getAppointmentsByPatientId(patientId, page, size);
        return ResponseEntity.ok(appointments.map(this::mapToResponse));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<Page<AppointmentResponse>> getDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Appointment> appointments;
        if (date != null) {
            appointments = appointmentService.getAppointmentsByDoctorAndDate(doctorId, date, page, size);
        } else {
            appointments = appointmentService.getAppointmentsByDoctorId(doctorId, page, size);
        }
        return ResponseEntity.ok(appointments.map(this::mapToResponse));
    }

    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @PatchMapping("/{appointmentId}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable Long appointmentId,
            @RequestParam AppointmentStatus status) {

        Appointment appointment = appointmentService.updateAppointmentStatus(appointmentId, status);
        return ResponseEntity.ok(mapToResponse(appointment));
    }

    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @GetMapping("/doctor/{doctorId}/available-slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<LocalTime> slots = appointmentService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(slots);
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/{doctorId}/search")
    public ResponseEntity<Page<AppointmentResponse>> searchDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Appointment> appointments = appointmentService.searchAppointments(doctorId, query, page, size);
        return ResponseEntity.ok(appointments.map(this::mapToResponse));
    }

    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/patient/{patientId}/upcoming")
    public ResponseEntity<Page<AppointmentResponse>> getUpcomingPatientAppointments(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        // Shield Check
        verifyPatientOwnership(patientId, authentication);

        Page<Appointment> appointments = appointmentService.getUpcomingAppointmentsForPatient(patientId, page, size);
        return ResponseEntity.ok(appointments.map(this::mapToResponse));
    }
}