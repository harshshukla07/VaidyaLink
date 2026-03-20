package com.vaidyalink.backend.repository;

import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import com.vaidyalink.backend.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

    Page<Appointment> findByDoctorId(Long doctorId, Pageable pageable);

    Page<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate appointmentDate, Pageable pageable);

    // This will return a list of appointments
    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusNot(Long doctorId, LocalDate appointmentDate, AppointmentStatus status);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(Long doctorId, LocalDate appointmentDate, LocalTime appointmentTime,  AppointmentStatus status);

    Page<Appointment> findByDoctorIdAndPatientNameContainingIgnoreCase(Long doctorId, String patientName, Pageable pageable);

    Page<Appointment> findByDoctorIdAndPatientMobile(Long doctorId, String patientMobile, Pageable pageable);

    @Query("""
    SELECT a
    FROM Appointment a
    WHERE a.patient.id = :patientId
      AND a.status IN :activeStatuses
      AND (
            a.appointmentDate > :today
            OR (a.appointmentDate = :today AND a.appointmentTime >= :now)
          )
    """)
    Page<Appointment> findUpcomingAppointmentsForPatient(
            @Param("patientId") Long patientId,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now,
            @Param("activeStatuses") List<AppointmentStatus> activeStatuses,
            Pageable pageable
    );

}
