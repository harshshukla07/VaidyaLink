package com.vaidyalink.backend.service;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.repository.AppointmentRepository;
import com.vaidyalink.backend.repository.DoctorRepository;
import com.vaidyalink.backend.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService{
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, PatientRepository patientRepository, DoctorRepository doctorRepository){
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

//    @Override
//    public Appointment bookAppointment(Appointment appointment){
//        return appointmentRepository.save(appointment);
//    }

    @Override
    public Appointment bookAppointment(AppointmentRequest request){

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (request.getAppointmentDate().equals(today)) {
            if (request.getAppointmentTime().isBefore(now)) {
                throw new IllegalStateException("You cannot book a past time for today's date.");
            }
        }

        int minutes = request.getAppointmentTime().getMinute();
        if (minutes != 0 && minutes != 20 && minutes != 40) {
            throw new IllegalStateException("Invalid slot! Appointments can only be booked at :00 or :20 or :40 minutes (e.g., 10:00, 10:20, 10:40).");
        }

        //Fetching patient by id
        Patient patient = patientRepository.findById(request.getPatientId()).orElseThrow(()-> new RuntimeException("Patient Not Found with id: "+request.getPatientId()));
        //Fetching Doctor by id
        Doctor doctor = doctorRepository.findById(request.getDoctorId()).orElseThrow(()-> new RuntimeException("Doctor Not Found with id: "+request.getDoctorId()));
        //Create new Appointment Object
        Appointment appointment = new Appointment();

        boolean isSlotTaken = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTime(request.getDoctorId(), request.getAppointmentDate(), request.getAppointmentTime());
        if(isSlotTaken){
            throw new IllegalStateException("Sorry, This slot for Dr." + doctor.getName() + " is already booked.");
        }

        //Set data using setters
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setStatus(AppointmentStatus.PENDING);

        return appointmentRepository.save(appointment);
    }

//    @Override
//    public List<Appointment> getAppointmentsByPatientId(Long patientId){
//        return appointmentRepository.findByPatientId(patientId);
//    }

    @Override
    public Page<Appointment> getAppointmentsByPatientId(Long patientId, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by("appointmentDate").descending()
        );

        return appointmentRepository.findByPatientId(patientId, pageable);
    }

//    public Page<Appointment> getAppointmentsForDoctor(Long doctorId, int page, int size) {
//        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
//
//        return appointmentRepository.findByDoctorId(doctorId, pageable);
//    }

//    @Override
//    public List<Appointment> getAppointmentsByDoctorId(Long doctorId){
//        return appointmentRepository.findByDoctorId(doctorId);
//    }

    @Override
    public Page<Appointment> getAppointmentsByDoctorId(Long doctorId, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("appointmentDate").descending());

        return appointmentRepository.findByDoctorId(doctorId, pageable);
    }

    @Override
    public Page<Appointment> getAppointmentsByDoctorAndDate(Long doctorId, LocalDate date, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("appointmentTime").ascending());

        return appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date,pageable);
    }

    @Override
    public Appointment updateAppointmentStatus(Long appointmentId, AppointmentStatus newStatus) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));


        AppointmentStatus appointmentStatus = appointment.getStatus();
        if(appointmentStatus == AppointmentStatus.CANCELLED || appointmentStatus == AppointmentStatus.COMPLETED){
            throw new IllegalStateException("Appointment Status is Cancelled or Completed");
        }
        else{
            appointment.setStatus(newStatus);
        }

        return appointmentRepository.save(appointment);
    }


}
