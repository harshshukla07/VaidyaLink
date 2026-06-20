package com.vaidyalink.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vaidyalink.backend.entity.Patient;

public interface PatientService {

    Patient getPatientById(Long id);

    Page<Patient> getAllPatients(Pageable pageable);
}
