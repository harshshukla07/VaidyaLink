package com.vaidyalink.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.repository.PatientRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientServiceImpl patientService;

    @Test
    void getPatientById_ShouldReturnPatient_WhenFound() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("Alice");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        Patient result = patientService.getPatientById(1L);

        assertEquals("Alice", result.getName());
    }

    @Test
    void getPatientById_ShouldThrow404_WhenNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> patientService.getPatientById(99L));
    }

    @Test
    void getAllPatients_ShouldReturnPageFromRepository() {
        Patient patient = new Patient();
        patient.setId(1L);
        Page<Patient> page = new PageImpl<>(java.util.List.of(patient));
        PageRequest pageable = PageRequest.of(0, 10);

        when(patientRepository.findAll(pageable)).thenReturn(page);

        Page<Patient> result = patientService.getAllPatients(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(patientRepository).findAll(pageable);
    }
}
