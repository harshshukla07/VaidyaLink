package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.service.PatientService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService){
        this.patientService = patientService;
    }

    @PostMapping("/register")
    public Patient registerPatient(@RequestBody Patient patient){
        return patientService.registerPatient(patient);
    }
}
