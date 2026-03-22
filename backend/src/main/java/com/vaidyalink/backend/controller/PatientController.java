package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.dto.PatientResponse;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.service.PatientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService){
        this.patientService = patientService;
    }

    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getPatientById(@PathVariable Long id){
        Patient patient = patientService.getPatientById(id);
        PatientResponse response = new PatientResponse(patient.getId(), patient.getName(), patient.getEmail(), patient.getMobile(), patient.getGender(), patient.getAge());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/all")
    public ResponseEntity<Page<PatientResponse>> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Patient> patientPage = patientService.getAllPatients(pageable);
        Page<PatientResponse> responsePage = patientPage
                .map(p -> new PatientResponse(p.getId(), p.getName(), p.getEmail(), p.getMobile(), p.getGender(), p.getAge()));
        return ResponseEntity.ok(responsePage);
    }
}