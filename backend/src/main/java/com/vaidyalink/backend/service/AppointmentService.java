package com.vaidyalink.backend.service;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentService {
//    Appointment bookAppointment(Appointment appointment);

    Appointment bookAppointment(AppointmentRequest request );

//    List<Appointment> getAppointmentsByPatientId(Long patientId);
    Page<Appointment> getAppointmentsByPatientId(Long patientId, int pageNumber, int pageSize);

//    List<Appointment> getAppointmentsByDoctorId(Long doctorId);
    Page<Appointment> getAppointmentsByDoctorId(Long doctorId, int pageNumber, int pageSize);

    Page<Appointment> getAppointmentsByDoctorAndDate(Long doctorId, LocalDate date, int page, int size);

    Appointment updateAppointmentStatus(Long appointmentId, AppointmentStatus newStatus);

    List<LocalTime> getAvailableSlots(Long doctorId, LocalDate date);

}
