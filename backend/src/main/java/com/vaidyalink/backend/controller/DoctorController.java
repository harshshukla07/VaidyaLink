package com.vaidyalink.backend.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vaidyalink.backend.dto.DoctorResponse;
import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.service.DoctorService;

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
    public ResponseEntity<Page<DoctorResponse>> getAllDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Doctor> doctorPage = doctorService.getAllDoctors(pageable);

        Page<DoctorResponse> responsePage = doctorPage.map(d ->
                new DoctorResponse(d.getId(), d.getName(), d.getEmail(), d.getSpeciality(), d.getExperience())
        );

        return ResponseEntity.ok(responsePage);
    }
}