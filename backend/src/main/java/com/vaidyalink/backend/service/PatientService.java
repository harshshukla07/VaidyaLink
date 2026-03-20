package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Patient;

import java.util.List;

public interface PatientService {

    Patient getPatientById(Long id);

    List<Patient> getAllPatients();
}
