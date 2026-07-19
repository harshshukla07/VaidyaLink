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
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorService doctorService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          PatientRepository patientRepository,
                          DoctorRepository doctorRepository,
                          PasswordEncoder passwordEncoder,
                          DoctorService doctorService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.doctorService = doctorService;
    }

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

            AuthResponse authResponse = new AuthResponse();
            authResponse.setToken(jwt);
            authResponse.setRole(role);
            authResponse.setEmail(loginRequest.getEmail());

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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid email or password");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Critical Login Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: An unexpected error occurred during login. Please try again later.");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        AuthResponse authResponse = new AuthResponse();
        authResponse.setEmail(email);
        authResponse.setRole(role);

        if (role.equals("ROLE_PATIENT")) {
            Patient patient = patientRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Patient data missing in system"));
            authResponse.setId(patient.getId());
            authResponse.setName(patient.getName());
        } else if (role.equals("ROLE_DOCTOR")) {
            Doctor doctor = doctorRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Doctor data missing in system"));
            authResponse.setId(doctor.getId());
            authResponse.setName(doctor.getName());
        }

        return ResponseEntity.ok(authResponse);
    }
}
