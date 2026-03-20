package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.dto.DoctorResponse;
import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService){
        this.doctorService = doctorService;
    }

    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable Long id){
        Doctor doctor = doctorService.getDoctorById(id);
        DoctorResponse response = new DoctorResponse(doctor.getId(), doctor.getName(), doctor.getEmail(), doctor.getSpeciality(), doctor.getExperience());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @GetMapping
    public ResponseEntity<List<DoctorResponse>> getDoctorsBySpeciality(@RequestParam String speciality){
        List<Doctor> doctors = doctorService.getDoctorsBySpeciality(speciality);
        List<DoctorResponse> responseList = doctors.stream()
                .map(d -> new DoctorResponse(d.getId(), d.getName(), d.getEmail(), d.getSpeciality(), d.getExperience()))
                .toList();
        return ResponseEntity.ok(responseList);
    }

    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @GetMapping("/all")
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        List<Doctor> doctors = doctorService.getAllDoctors();
        List<DoctorResponse> responseList = doctors.stream()
                .map(d -> new DoctorResponse(d.getId(), d.getName(), d.getEmail(), d.getSpeciality(), d.getExperience()))
                .toList();
        return ResponseEntity.ok(responseList);
    }
}