package com.vaidyalink.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.entity.DoctorSlot;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.entity.SlotStatus;
import com.vaidyalink.backend.repository.AppointmentRepository;
import com.vaidyalink.backend.repository.DoctorRepository;
import com.vaidyalink.backend.repository.DoctorSlotRepository;
import com.vaidyalink.backend.repository.PatientRepository;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorSlotRepository doctorSlotRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Patient mockPatient;
    private Doctor mockDoctor;
    private DoctorSlot mockSlot;
    private AppointmentRequest baseRequest;

    @BeforeEach
    void setUp() {
        mockPatient = new Patient();
        mockPatient.setId(1L);
        mockPatient.setName("Alice Patient");

        mockDoctor = new Doctor();
        mockDoctor.setId(1L);
        mockDoctor.setName("Dr. Sharma");

        mockSlot = new DoctorSlot();
        mockSlot.setDoctor(mockDoctor);
        mockSlot.setSlotDate(LocalDate.now().plusDays(1));
        mockSlot.setStartTime(LocalTime.of(10, 0));
        mockSlot.setEndTime(LocalTime.of(10, 20));
        mockSlot.setStatus(SlotStatus.AVAILABLE);

        baseRequest = new AppointmentRequest();
        baseRequest.setPatientId(1L);
        baseRequest.setDoctorId(1L);
        baseRequest.setAppointmentDate(LocalDate.now().plusDays(1));
        baseRequest.setAppointmentTime(LocalTime.of(10, 0));
    }

    @Test
    void bookAppointment_ShouldSaveAndReturnAppointment_WhenSlotIsAvailable() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
        when(doctorSlotRepository.findByDoctorIdAndSlotDateAndStartTime(
                eq(1L), eq(baseRequest.getAppointmentDate()), eq(baseRequest.getAppointmentTime())))
                .thenReturn(Optional.of(mockSlot));

        Appointment savedAppt = new Appointment();
        savedAppt.setId(100L);
        savedAppt.setStatus(AppointmentStatus.PENDING);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppt);
        when(doctorSlotRepository.save(any(DoctorSlot.class))).thenReturn(mockSlot);

        Appointment result = appointmentService.bookAppointment(baseRequest);

        assertNotNull(result);
        assertEquals(AppointmentStatus.PENDING, result.getStatus());
        assertEquals(100L, result.getId());
        verify(doctorSlotRepository, times(1)).save(any(DoctorSlot.class));
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    void bookAppointment_ShouldThrowException_WhenPatientNotFound() {
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> appointmentService.bookAppointment(baseRequest));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookAppointment_ShouldThrowException_WhenSlotNotGenerated() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
        when(doctorSlotRepository.findByDoctorIdAndSlotDateAndStartTime(
                anyLong(), any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> appointmentService.bookAppointment(baseRequest));

        assertEquals("Slot not generated or invalid time for this doctor.", ex.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookAppointment_ShouldThrowException_WhenSlotAlreadyBooked() {
        mockSlot.setStatus(SlotStatus.BOOKED);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
        when(doctorSlotRepository.findByDoctorIdAndSlotDateAndStartTime(
                eq(1L), eq(baseRequest.getAppointmentDate()), eq(baseRequest.getAppointmentTime())))
                .thenReturn(Optional.of(mockSlot));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> appointmentService.bookAppointment(baseRequest));

        assertEquals("Sorry, This slot is already booked.", ex.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void updateAppointmentStatus_ShouldUpdateStatus_WhenStatusIsNotTerminal() {
        Appointment pendingAppt = new Appointment();
        pendingAppt.setId(10L);
        pendingAppt.setStatus(AppointmentStatus.PENDING);

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(pendingAppt));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(pendingAppt);

        Appointment updated = appointmentService.updateAppointmentStatus(10L, AppointmentStatus.CONFIRMED);

        assertEquals(AppointmentStatus.CONFIRMED, updated.getStatus());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    void updateAppointmentStatus_ShouldThrowException_WhenStatusIsAlreadyCancelled() {
        Appointment cancelledAppt = new Appointment();
        cancelledAppt.setId(20L);
        cancelledAppt.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(20L)).thenReturn(Optional.of(cancelledAppt));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> appointmentService.updateAppointmentStatus(20L, AppointmentStatus.CONFIRMED));

        assertEquals("Appointment Status is Cancelled or Completed", ex.getMessage());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void getAvailableSlots_ShouldReturnAvailableSlotTimes() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        DoctorSlot slot1 = new DoctorSlot();
        slot1.setStartTime(LocalTime.of(10, 0));
        DoctorSlot slot2 = new DoctorSlot();
        slot2.setStartTime(LocalTime.of(10, 20));

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(doctorSlotRepository.findByDoctorIdAndSlotDateAndStatus(1L, tomorrow, SlotStatus.AVAILABLE))
                .thenReturn(List.of(slot1, slot2));

        List<LocalTime> availableSlots = appointmentService.getAvailableSlots(1L, tomorrow);

        assertEquals(2, availableSlots.size());
        assertTrue(availableSlots.contains(LocalTime.of(10, 0)));
        assertTrue(availableSlots.contains(LocalTime.of(10, 20)));
    }

    @Test
    void getAvailableSlots_ShouldExcludePastTimesForToday() {
        LocalDate today = LocalDate.now();

        DoctorSlot pastSlot = new DoctorSlot();
        pastSlot.setStartTime(LocalTime.now().minusHours(2));
        DoctorSlot futureSlot = new DoctorSlot();
        futureSlot.setStartTime(LocalTime.of(23, 59));

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(doctorSlotRepository.findByDoctorIdAndSlotDateAndStatus(1L, today, SlotStatus.AVAILABLE))
                .thenReturn(List.of(pastSlot, futureSlot));

        List<LocalTime> availableSlots = appointmentService.getAvailableSlots(1L, today);

        assertFalse(availableSlots.contains(pastSlot.getStartTime()));
        assertTrue(availableSlots.contains(futureSlot.getStartTime()));
    }
}
