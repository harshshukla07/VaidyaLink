package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.repository.DoctorRepository;
import org.springframework.cache.annotation.Cacheable;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService{
    private final DoctorRepository doctorRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository){
        this.doctorRepository = doctorRepository;
    }

    @Override
    public List<Doctor> getDoctorsBySpeciality(String speciality){
        return doctorRepository.findBySpeciality(speciality);
    }

    @Override
    @Cacheable(value = "doctors_page", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Doctor> getAllDoctors(Pageable pageable) {
        return doctorRepository.findAll(pageable);
    }

    @Override
    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("No patient found with this particular Id: " + id));
    }

}
