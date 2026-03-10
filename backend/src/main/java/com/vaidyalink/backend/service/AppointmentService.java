package com.vaidyalink.backend.service;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;

import java.util.List;

public interface AppointmentService {
//    Appointment bookAppointment(Appointment appointment);

    Appointment bookAppointment(AppointmentRequest request );

    List<Appointment> getAppointmentsByPatientId(Long patientId);

    List<Appointment> getAppointmentsByDoctorId(Long doctorId);

    Appointment updateAppointmentStatus(Long appointmentId, AppointmentStatus newStatus);

}
