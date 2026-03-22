package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PatientService {

    Patient getPatientById(Long id);

    Page<Patient> getAllPatients(Pageable pageable);
}
