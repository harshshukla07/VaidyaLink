package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

@Service
public class AppointmentServiceImpl implements AppointmentService{
    private final AppointmentRepository appointmentRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository){
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public Appointment bookAppointment(Appointment appointment){
        return appointmentRepository.save(appointment);
    }
}
