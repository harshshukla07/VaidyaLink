package com.vaidyalink.backend.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.repository.DoctorRepository;

import jakarta.persistence.EntityNotFoundException;

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

    @Override
    @CacheEvict(value = {"doctors_page","distinct_specialities"}, allEntries = true)
    public Doctor registerNewDoctor(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

    @Override
    @Cacheable(value="distinct_specialities")
    public List<String> getDistinctSpecialities() {
        return doctorRepository.findDistinctSpecialities();
    }

}
