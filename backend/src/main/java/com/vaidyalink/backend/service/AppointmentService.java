package com.vaidyalink.backend.service;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AppointmentService {
//    Appointment bookAppointment(Appointment appointment);

    Appointment bookAppointment(AppointmentRequest request );

//    List<Appointment> getAppointmentsByPatientId(Long patientId);
    Page<Appointment> getAppointmentsByPatientId(Long patientId, int pageNumber, int pageSize);

//    List<Appointment> getAppointmentsByDoctorId(Long doctorId);
    Page<Appointment> getAppointmentsByDoctorId(Long doctorId, int pageNumber, int pageSize);

    Appointment updateAppointmentStatus(Long appointmentId, AppointmentStatus newStatus);

}
