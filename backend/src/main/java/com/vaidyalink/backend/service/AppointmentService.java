package com.vaidyalink.backend.service;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;

public interface AppointmentService {
//    Appointment bookAppointment(Appointment appointment);

    Appointment bookAppointment(AppointmentRequest request );
}
