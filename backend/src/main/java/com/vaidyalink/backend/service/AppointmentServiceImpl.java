package com.vaidyalink.backend.service;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.repository.AppointmentRepository;
import com.vaidyalink.backend.repository.DoctorRepository;
import com.vaidyalink.backend.repository.PatientRepository;
import org.springframework.stereotype.Service;

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
        //Fetching patient by id
        Patient patient = patientRepository.findById(request.getPatientId()).orElseThrow(()-> new RuntimeException("Patient Not Found with id: "+request.getPatientId()));
        //Fetching Doctor by id
        Doctor doctor = doctorRepository.findById(request.getDoctorId()).orElseThrow(()-> new RuntimeException("Doctor Not Found with id: "+request.getDoctorId()));
        //Create new Appointment Object
        Appointment appointment = new Appointment();

        //Set data using setters
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setStatus("PENDING");

        return appointmentRepository.save(appointment);
    }

    @Override
    public List<Appointment> getAppointmentsByPatientId(Long patientId){
        return appointmentRepository.findByPatientId(patientId);
    }

    @Override
    public List<Appointment> getAppointmentsByDoctorId(Long doctorId){
        return appointmentRepository.findByDoctorId(doctorId);
    }
}
