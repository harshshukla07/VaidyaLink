package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.service.AppointmentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService){
        this.appointmentService = appointmentService;
    }

    @PostMapping("/book")
    public Appointment bookAppointment(@RequestBody AppointmentRequest request){
        return appointmentService.bookAppointment(request);
    }
}
