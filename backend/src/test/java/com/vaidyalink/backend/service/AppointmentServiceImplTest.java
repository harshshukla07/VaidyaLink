package com.vaidyalink.backend.service;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.repository.AppointmentRepository;
import com.vaidyalink.backend.repository.DoctorRepository;
import com.vaidyalink.backend.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Patient mockPatient;
    private Doctor mockDoctor;
    private AppointmentRequest baseRequest;

    @BeforeEach
    void setUp() {
        // Creating mock data before test
        mockPatient = new Patient();
        mockPatient.setId(1L);
        mockPatient.setName("Harsh Shukla");

        mockDoctor = new Doctor();
        mockDoctor.setId(1L);
        mockDoctor.setName("Dr. Sharma");

        baseRequest = new AppointmentRequest();
        baseRequest.setPatientId(1L);
        baseRequest.setDoctorId(1L);
        // Default valid request: yesterday's date, valid slot (10:20 AM)
        baseRequest.setAppointmentDate(LocalDate.now().plusDays(1));
        baseRequest.setAppointmentTime(LocalTime.of(10, 20));
    }

    // TEST 1: Appointment booking should succeed when request is valid and all preconditions are met
    @Test
    void bookAppointment_ShouldSaveAndReturnAppointment_WhenValidRequest() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));

        // Mock that the requested slot is available
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                anyLong(), any(LocalDate.class), any(LocalTime.class), any(AppointmentStatus.class)
        )).thenReturn(false);

        // Prepare mock response that will be returned from repository save
        Appointment savedAppt = new Appointment();
        savedAppt.setId(100L);
        savedAppt.setStatus(AppointmentStatus.PENDING);
        savedAppt.setAppointmentDate(baseRequest.getAppointmentDate());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppt);

        // Execute the booking
        Appointment result = appointmentService.bookAppointment(baseRequest);

        // Verify the returned appointment has correct properties
        assertNotNull(result);
        assertEquals(AppointmentStatus.PENDING, result.getStatus());
        assertEquals(100L, result.getId());
        // Verify that the repository save method was called exactly once
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    // TEST 2: Past time booking should be rejected for today's date
    @Test
    void bookAppointment_ShouldThrowException_WhenBookingPastTimeToday() {
        baseRequest.setAppointmentDate(LocalDate.now());
        // Set appointment time to one hour in the past
        baseRequest.setAppointmentTime(LocalTime.now().minusHours(1));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            appointmentService.bookAppointment(baseRequest);
        });

        assertEquals("You cannot book a past time for today's date.", exception.getMessage());
        // Verify that database save was never called (fail-fast validation)
        verify(appointmentRepository, never()).save(any());
    }

    // TEST 3: Invalid slot times should be rejected (only :00, :20, :40 are valid)
    @Test
    void bookAppointment_ShouldThrowException_WhenSlotIsInvalid() {
        // Request slot at 10:15, which is not a valid boundary
        baseRequest.setAppointmentTime(LocalTime.of(10, 15));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            appointmentService.bookAppointment(baseRequest);
        });

        assertTrue(exception.getMessage().contains("Invalid slot!"));
        verify(appointmentRepository, never()).save(any());
    }

    // TEST 4: Double booking for the same doctor/date/time should be prevented
    @Test
    void bookAppointment_ShouldThrowException_WhenSlotIsAlreadyTaken() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));

        // Mock that the requested slot is already booked for this doctor
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                eq(1L), eq(baseRequest.getAppointmentDate()), eq(baseRequest.getAppointmentTime()), eq(AppointmentStatus.CANCELLED)
        )).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            appointmentService.bookAppointment(baseRequest);
        });

        assertEquals("Sorry, This slot for Dr.Dr. Sharma is already booked.", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    // TEST 5: Status update should succeed when current status is not terminal
    @Test
    void updateAppointmentStatus_ShouldUpdateStatus_WhenStatusIsNotTerminal() {
        // Setup a pending appointment
        Appointment pendingAppt = new Appointment();
        pendingAppt.setId(10L);
        pendingAppt.setStatus(AppointmentStatus.PENDING);

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(pendingAppt));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(pendingAppt);

        // Transition from PENDING to CONFIRMED
        Appointment updatedAppt = appointmentService.updateAppointmentStatus(10L, AppointmentStatus.CONFIRMED);

        // Verify the status was updated correctly
        assertEquals(AppointmentStatus.CONFIRMED, updatedAppt.getStatus());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    // TEST 6: Status update should be rejected when appointment is already cancelled
    @Test
    void updateAppointmentStatus_ShouldThrowException_WhenStatusIsAlreadyCancelled() {
        // Setup a cancelled appointment
        Appointment cancelledAppt = new Appointment();
        cancelledAppt.setId(20L);
        cancelledAppt.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(20L)).thenReturn(Optional.of(cancelledAppt));

        // Attempt to update a cancelled appointment should fail
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            appointmentService.updateAppointmentStatus(20L, AppointmentStatus.CONFIRMED);
        });

        assertEquals("Appointment Status is Cancelled or Completed", exception.getMessage());
        // Verify that save was never called since the state transition is invalid
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    // TEST 7: Available slots should only include unbooked slots for the given date
    @Test
    void getAvailableSlots_ShouldReturnOnlyFreeSlots_ForFutureDate() {
        // Setup doctor
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));

        // Check for tomorrow's date
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Assume doctor has two appointments booked tomorrow: 10:00 AM and 6:00 PM
        Appointment appt1 = new Appointment();
        appt1.setAppointmentTime(LocalTime.of(10, 0));
        Appointment appt2 = new Appointment();
        appt2.setAppointmentTime(LocalTime.of(18, 0));

        when(appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusNot(
                1L, tomorrow, AppointmentStatus.CANCELLED
        )).thenReturn(List.of(appt1, appt2));

        // Execute
        List<LocalTime> availableSlots = appointmentService.getAvailableSlots(1L, tomorrow);

        // Verify results
        assertNotNull(availableSlots);
        // Morning shift (9 slots) + Evening shift (9 slots) = 18 total. With 2 booked, 16 should remain
        assertEquals(16, availableSlots.size());

        // Verify that booked slots are not in the available list
        assertFalse(availableSlots.contains(LocalTime.of(10, 0)));
        assertFalse(availableSlots.contains(LocalTime.of(18, 0)));

        // Verify that the next free slot after the first booking is available
        assertTrue(availableSlots.contains(LocalTime.of(10, 20)));
    }
}