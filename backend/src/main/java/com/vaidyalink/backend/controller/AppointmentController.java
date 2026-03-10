package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import com.vaidyalink.backend.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
    public List<Appointment> getPatientAppointments(@PathVariable Long patientId) {
        return appointmentService.getAppointmentsByPatientId(patientId);
    }

    @GetMapping("/doctor/{doctorId}")
    public List<Appointment> getDoctorAppointments(@PathVariable Long doctorId) {
        return appointmentService.getAppointmentsByDoctorId(doctorId);
    }

    @PatchMapping("/{appointmentId}/status")
    public Appointment updateStatus(
            @PathVariable Long appointmentId,
            @RequestParam AppointmentStatus status) {

        return appointmentService.updateAppointmentStatus(appointmentId, status);
    }
}
