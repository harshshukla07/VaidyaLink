package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.repository.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Patient registerPatient(Patient patient) {
        return patientRepository.save(patient);
    }
}