package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.dto.DoctorRegisterRequest;
import com.vaidyalink.backend.dto.LoginRequest;
import com.vaidyalink.backend.dto.PatientRegisterRequest;
import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.repository.DoctorRepository;
import com.vaidyalink.backend.repository.PatientRepository;
import com.vaidyalink.backend.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. Patient Registration API
    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody PatientRegisterRequest request) {

        // Pehle check karo ki email already exist toh nahi karta
        if (patientRepository.findByEmail(request.getEmail()).isPresent() ||
                doctorRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Email is already registered!");
        }

        // DTO se data nikal kar Entity mein daalo
        Patient patient = new Patient();
        patient.setName(request.getName());
        patient.setEmail(request.getEmail());
        patient.setMobile(request.getMobile());
        patient.setGender(request.getGender());
        patient.setAge(request.getAge());

        // SDE Rule: Password hamesha encrypt karke save hoga!
        patient.setPassword(passwordEncoder.encode(request.getPassword()));

        patientRepository.save(patient);
        return ResponseEntity.ok("Patient registered successfully!");
    }

    // 2. Doctor Registration API
    @PostMapping("/register/doctor")
    public ResponseEntity<?> registerDoctor(@Valid @RequestBody DoctorRegisterRequest request) {

        if (doctorRepository.findByEmail(request.getEmail()).isPresent() ||
                patientRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Email is already registered!");
        }

        Doctor doctor = new Doctor();
        doctor.setName(request.getName());
        doctor.setEmail(request.getEmail());
        doctor.setSpeciality(request.getSpeciality());
        doctor.setExperience(request.getExperience());

        // SDE Rule: Encrypt the password
        doctor.setPassword(passwordEncoder.encode(request.getPassword()));

        doctorRepository.save(doctor);
        return ResponseEntity.ok("Doctor registered successfully!");
    }

    // 3. The Login API (Generates Token)
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Spring Security ko bolo: "Bhai, ye email aur password check karke bata DB mein hai ya nahi"
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            // Agar yahan tak code aa gaya, matlab credentials sahi hain. Security Context set karo.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Token generate karo
            String jwt = jwtUtil.generateToken(loginRequest.getEmail());

            // Token ko JSON format mein return karo
            Map<String, String> response = new HashMap<>();
            response.put("token", jwt);
            response.put("message", "Login successful");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Agar password galat hua ya email nahi mila, toh 401 Unauthorized de do
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid email or password");
        }
    }
}