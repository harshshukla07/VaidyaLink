package com.vaidyalink.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.repository.DoctorRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    @Test
    void getDoctorsBySpeciality_ShouldReturnList() {
        Doctor doctor = new Doctor();
        doctor.setSpeciality("Cardiology");
        when(doctorRepository.findBySpeciality("Cardiology")).thenReturn(List.of(doctor));

        List<Doctor> result = doctorService.getDoctorsBySpeciality("Cardiology");

        assertEquals(1, result.size());
        assertEquals("Cardiology", result.get(0).getSpeciality());
    }

    @Test
    void getDoctorById_ShouldReturnDoctor_WhenFound() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Smith");
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        Doctor result = doctorService.getDoctorById(1L);

        assertEquals("Dr. Smith", result.getName());
    }

    @Test
    void getDoctorById_ShouldThrow404_WhenNotFound() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> doctorService.getDoctorById(99L));
    }

    @Test
    void registerNewDoctor_ShouldSaveDoctor() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. New");
        when(doctorRepository.save(doctor)).thenReturn(doctor);

        Doctor result = doctorService.registerNewDoctor(doctor);

        assertNotNull(result);
        verify(doctorRepository).save(doctor);
    }

    @Test
    void getAllDoctors_ShouldReturnPage() {
        Doctor doctor = new Doctor();
        PageRequest pageable = PageRequest.of(0, 10);
        when(doctorRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(doctor)));

        Page<Doctor> result = doctorService.getAllDoctors(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getDistinctSpecialities_ShouldReturnDistinctList() {
        when(doctorRepository.findDistinctSpecialities())
                .thenReturn(List.of("Dermatology", "General Physician", "Orthopedics"));

        List<String> result = doctorService.getDistinctSpecialities();

        assertEquals(3, result.size());
        assertEquals("Dermatology", result.get(0));
        verify(doctorRepository).findDistinctSpecialities();
    }
}
