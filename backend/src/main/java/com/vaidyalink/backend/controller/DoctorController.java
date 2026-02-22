package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.service.DoctorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService){
        this.doctorService = doctorService;
    }

    @PostMapping("/register")
    public Doctor registerDoctor(@RequestBody Doctor doctor){
        return doctorService.registerDoctor(doctor);
    }

    @GetMapping
    public List<Doctor> getDoctorsBySpeciality(@RequestParam String speciality){
        return doctorService.getDoctorsBySpeciality(speciality);
    }
}
