package com.vaidyalink.backend.repository;

import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDoctor(Doctor doctor);
//    List<Appointment> findByPatientId(Long patientId);
    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);
//    List<Appointment> findByDoctorId(Long doctorId);
    Page<Appointment> findByDoctorId(Long doctorId, Pageable pageable);

    Page<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate appointmentDate, Pageable pageable);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTime(Long doctorId, LocalDate appointmentDate, LocalTime appointmentTime);
}
