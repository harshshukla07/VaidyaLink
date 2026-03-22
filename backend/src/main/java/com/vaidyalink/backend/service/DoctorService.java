package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DoctorService {

    List<Doctor> getDoctorsBySpeciality(String speciality);
    Page<Doctor> getAllDoctors(Pageable pageable);
    Doctor getDoctorById(Long id);
}
