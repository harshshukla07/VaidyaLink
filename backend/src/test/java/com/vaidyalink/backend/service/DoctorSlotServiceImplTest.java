package com.vaidyalink.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.entity.DoctorSlot;
import com.vaidyalink.backend.entity.SlotStatus;
import com.vaidyalink.backend.repository.DoctorRepository;
import com.vaidyalink.backend.repository.DoctorSlotRepository;

@ExtendWith(MockitoExtension.class)
class DoctorSlotServiceImplTest {

    @Mock
    private DoctorSlotRepository doctorSlotRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorSlotServiceImpl doctorSlotService;

    private Doctor doctor;

    @BeforeEach
    void setUp() {
        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Sharma");
    }

    @Test
    void generateSlotsForDay_ShouldCreateSlotsForShift() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorSlotRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<DoctorSlot> slots = doctorSlotService.generateSlotsForDay(1L, tomorrow, start, end, 20);

        assertEquals(3, slots.size());
        assertEquals(SlotStatus.AVAILABLE, slots.get(0).getStatus());
        assertEquals(LocalTime.of(9, 0), slots.get(0).getStartTime());
        verify(doctorSlotRepository).saveAll(anyList());
    }

    @Test
    void generateSlotsForDay_ShouldThrow_WhenShiftIsInPastToday() {
        LocalDate today = LocalDate.now();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> doctorSlotService.generateSlotsForDay(
                        1L, today, LocalTime.now().minusHours(2), LocalTime.of(18, 0), 20));

        assertTrue(ex.getMessage().contains("past"));
    }

    @Test
    void generateSlotsForDay_ShouldThrow_WhenDoctorNotFound() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> doctorSlotService.generateSlotsForDay(
                        99L, LocalDate.now().plusDays(1),
                        LocalTime.of(9, 0), LocalTime.of(12, 0), 20));
    }
}
