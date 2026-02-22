package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService{
    private final DoctorRepository doctorRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository){
        this.doctorRepository = doctorRepository;
    }

    @Override
    public Doctor registerDoctor(Doctor doctor){
        return doctorRepository.save(doctor);
    }

    @Override
    public List<Doctor> getDoctorsBySpeciality(String speciality){
        return doctorRepository.findBySpeciality(speciality);
    }

}
