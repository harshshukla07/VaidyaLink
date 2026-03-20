package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import com.vaidyalink.backend.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService){
        this.appointmentService = appointmentService;
    }

    @PostMapping("/book")
    public ResponseEntity<Appointment> bookAppointment(@Valid @RequestBody AppointmentRequest request){
        Appointment appointment = appointmentService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<Appointment>> getPatientAppointments(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Appointment> appointments = appointmentService.getAppointmentsByPatientId(patientId, page, size);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<Page<Appointment>> getDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (date != null) {
            Page<Appointment> appointments = appointmentService.getAppointmentsByDoctorAndDate(doctorId, date, page, size);
            return ResponseEntity.ok(appointments);
        }

        Page<Appointment> appointments = appointmentService.getAppointmentsByDoctorId(doctorId, page, size);
        return ResponseEntity.ok(appointments);
    }

    @PatchMapping("/{appointmentId}/status")
    public ResponseEntity<Appointment> updateStatus(
            @PathVariable Long appointmentId,
            @RequestParam AppointmentStatus status) {

        Appointment appointment = appointmentService.updateAppointmentStatus(appointmentId, status);
        return ResponseEntity.ok(appointment);
    }

    @GetMapping("/doctor/{doctorId}/available-slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<LocalTime> slots = appointmentService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/doctor/{doctorId}/search")
    public ResponseEntity<Page<Appointment>> searchDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Appointment> appointments = appointmentService.searchAppointments(doctorId, query, page, size);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/patient/{patientId}/upcoming")
    public ResponseEntity<Page<Appointment>> getUpcomingPatientAppointments(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Appointment> appointments = appointmentService.getUpcomingAppointmentsForPatient(patientId, page, size);
        return ResponseEntity.ok(appointments);
    }
}