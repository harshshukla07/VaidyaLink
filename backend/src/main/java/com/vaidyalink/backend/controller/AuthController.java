package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.dto.AuthResponse;
import com.vaidyalink.backend.dto.DoctorRegisterRequest;
import com.vaidyalink.backend.dto.LoginRequest;
import com.vaidyalink.backend.dto.PatientRegisterRequest;
import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.repository.DoctorRepository;
import com.vaidyalink.backend.repository.PatientRepository;
import com.vaidyalink.backend.security.JwtUtil;
import com.vaidyalink.backend.service.DoctorService;
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
import org.springframework.security.authentication.BadCredentialsException;
import jakarta.persistence.EntityNotFoundException;


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

    @Autowired
    private DoctorService doctorService;

    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody PatientRegisterRequest request) {

        if (patientRepository.findByEmail(request.getEmail()).isPresent() ||
                doctorRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Email is already registered!");
        }

        Patient patient = new Patient();
        patient.setName(request.getName());
        patient.setEmail(request.getEmail());
        patient.setMobile(request.getMobile());
        patient.setGender(request.getGender());
        patient.setAge(request.getAge());

        patient.setPassword(passwordEncoder.encode(request.getPassword()));

        patientRepository.save(patient);
        return ResponseEntity.ok("Patient registered successfully!");
    }

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

        doctor.setPassword(passwordEncoder.encode(request.getPassword()));

//        doctorRepository.save(doctor);
        doctorService.registerNewDoctor(doctor);
        return ResponseEntity.ok("Doctor registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtil.generateToken(loginRequest.getEmail());
            String role = authentication.getAuthorities().iterator().next().getAuthority();

            // Made a new DTO
            AuthResponse authResponse = new AuthResponse();
            authResponse.setToken(jwt);
            authResponse.setRole(role);
            authResponse.setEmail(loginRequest.getEmail());

            // Fetch additional user details based on their role to enrich the auth payload
            if (role.equals("ROLE_PATIENT")) {
                Patient patient = patientRepository.findByEmail(loginRequest.getEmail())
                        .orElseThrow(() -> new EntityNotFoundException("Patient data missing in system"));
                authResponse.setId(patient.getId());
                authResponse.setName(patient.getName());
            } else if (role.equals("ROLE_DOCTOR")) {
                Doctor doctor = doctorRepository.findByEmail(loginRequest.getEmail())
                        .orElseThrow(() -> new EntityNotFoundException("Doctor data missing in system"));
                authResponse.setId(doctor.getId());
                authResponse.setName(doctor.getName());
            }

            return ResponseEntity.ok(authResponse);

        } catch (BadCredentialsException e) {
            // Catch only authentication failures (wrong email/password)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid email or password");
        } catch (EntityNotFoundException e) {
            // Catch missing database records after successful authentication
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (Exception e) {
            // Catch database errors or any other unexpected issues
            System.err.println("Critical Login Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: An unexpected error occurred during login. Please try again later.");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        // Get email and role from the 'authentication' object
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        AuthResponse authResponse = new AuthResponse();
        authResponse.setEmail(email);
        authResponse.setRole(role);

        if(role.equals("ROLE_PATIENT")){
            Patient patient = patientRepository.findByEmail(email)
                    .orElseThrow(()->new EntityNotFoundException("Patient data missing in system"));
            authResponse.setId(patient.getId());
            authResponse.setName(patient.getName());
        }else if(role.equals("ROLE_DOCTOR")){
            Doctor doctor = doctorRepository.findByEmail(email)
                    .orElseThrow(()->new EntityNotFoundException("Doctor data missing in system"));
            authResponse.setId(doctor.getId());
            authResponse.setName(doctor.getName());
        }

        return ResponseEntity.ok(authResponse);
    }
}