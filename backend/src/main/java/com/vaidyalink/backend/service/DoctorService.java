package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Doctor;

import java.util.List;

public interface DoctorService {

    List<Doctor> getDoctorsBySpeciality(String speciality);
    List<Doctor> getAllDoctors();
    Doctor getDoctorById(Long id);
}
