package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService){
        this.patientService = patientService;
    }

    @PostMapping("/register")
    public Patient registerPatient(@Valid @RequestBody Patient patient){
        return patientService.registerPatient(patient);
    }

    @GetMapping("/{id}")
    public Patient getPatientById(@PathVariable Long id){
        return patientService.getPatientById(id);
    }
}
