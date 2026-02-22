package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Doctor;

import java.util.List;

public interface DoctorService {
    Doctor registerDoctor(Doctor doctor);
    List<Doctor> getDoctorsBySpeciality(String speciality);
}
