package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import com.vaidyalink.backend.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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
    public Appointment bookAppointment(@Valid @RequestBody AppointmentRequest request){
        return appointmentService.bookAppointment(request);
    }

    @GetMapping("/patient/{patientId}")
    public Page<Appointment> getPatientAppointments(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return appointmentService.getAppointmentsByPatientId(patientId, page, size);
    }

    @GetMapping("/doctor/{doctorId}")
    public Page<Appointment> getDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (date != null) {
            return appointmentService.getAppointmentsByDoctorAndDate(doctorId, date, page, size);
        }

        return appointmentService.getAppointmentsByDoctorId(doctorId, page, size);
    }

    @PatchMapping("/{appointmentId}/status")
    public Appointment updateStatus(
            @PathVariable Long appointmentId,
            @RequestParam AppointmentStatus status) {

        return appointmentService.updateAppointmentStatus(appointmentId, status);
    }

    @GetMapping("/doctor/{doctorId}/available-slots")
    public List<LocalTime> getAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return appointmentService.getAvailableSlots(doctorId, date);
    }

    @GetMapping("/doctor/{doctorId}/search")
    public Page<Appointment> searchDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return appointmentService.searchAppointments(doctorId, query, page, size);
    }
}
