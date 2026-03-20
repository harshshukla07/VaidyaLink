package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Patient getPatientById(Long id){
        return patientRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("No patient found with this particular Id: " + id));
    }

    @Override
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }
}