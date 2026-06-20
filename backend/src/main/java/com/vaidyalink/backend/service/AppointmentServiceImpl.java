package com.vaidyalink.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import com.vaidyalink.backend.entity.DoctorSlot;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.entity.SlotStatus;
import com.vaidyalink.backend.repository.AppointmentRepository;
import com.vaidyalink.backend.repository.DoctorRepository;
import com.vaidyalink.backend.repository.DoctorSlotRepository;
import com.vaidyalink.backend.repository.PatientRepository;

import jakarta.transaction.Transactional;

@Service
public class AppointmentServiceImpl implements AppointmentService{
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorSlotRepository doctorSlotRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, PatientRepository patientRepository, DoctorRepository doctorRepository, DoctorSlotRepository doctorSlotRepository){
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.doctorSlotRepository = doctorSlotRepository;
    }

    @Override
    @Transactional
    public Appointment bookAppointment(AppointmentRequest request) {

        // Fetch Patient
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient Not Found with id: " + request.getPatientId()));

        // Fetch the specific DoctorSlot
        DoctorSlot slot = doctorSlotRepository.findByDoctorIdAndSlotDateAndStartTime(
                request.getDoctorId(), request.getAppointmentDate(), request.getAppointmentTime()
        ).orElseThrow(() -> new IllegalStateException("Slot not generated or invalid time for this doctor."));

        // The Check
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new IllegalStateException("Sorry, This slot is already booked.");
        }

        // Lock and Update Slot
        slot.setStatus(SlotStatus.BOOKED);
        doctorSlotRepository.save(slot);

        // Generate the Real Appointment Record
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(slot.getDoctor());
        appointment.setAppointmentDate(slot.getSlotDate());
        appointment.setAppointmentTime(slot.getStartTime());
        appointment.setStatus(AppointmentStatus.PENDING);

        // Save in Appointments Table
        Appointment savedAppointment = appointmentRepository.save(appointment);
        
        return savedAppointment;
    }


    @Override
    public Page<Appointment> getAppointmentsByPatientId(Long patientId, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by("appointmentDate").descending()
        );

        return appointmentRepository.findByPatientId(patientId, pageable);
    }

    @Override
    public Page<Appointment> getAppointmentsByDoctorId(Long doctorId, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("appointmentDate").descending());

        return appointmentRepository.findByDoctorId(doctorId, pageable);
    }

    @Override
    public Page<Appointment> getAppointmentsByDoctorAndDate(Long doctorId, LocalDate date, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("appointmentTime").ascending());

        return appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date,pageable);
    }

    @Override
    public Appointment updateAppointmentStatus(Long appointmentId, AppointmentStatus newStatus) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));


        AppointmentStatus appointmentStatus = appointment.getStatus();
        if(appointmentStatus == AppointmentStatus.CANCELLED || appointmentStatus == AppointmentStatus.COMPLETED){
            throw new IllegalStateException("Appointment Status is Cancelled or Completed");
        }
        else{
            appointment.setStatus(newStatus);
        }

        return appointmentRepository.save(appointment);
    }

    @Override
    public List<LocalTime> getAvailableSlots(Long doctorId, LocalDate date) {

        // Check if doctor exists
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Doctor not found with ID: " + doctorId));

        // Fetch ONLY explicitly generated and AVAILABLE slots from the DoctorSlot table
        List<DoctorSlot> availableSlotsList = doctorSlotRepository.findByDoctorIdAndSlotDateAndStatus(
                doctorId, date, SlotStatus.AVAILABLE);

        // Extract times and filter out past times if the date is today
        boolean isToday = date.equals(LocalDate.now());
        LocalTime now = LocalTime.now();

        return availableSlotsList.stream()
                .map(DoctorSlot::getStartTime)
                .filter(time -> !isToday || time.isAfter(now)) // only future time allowed
                .toList(); // Immutable list for safety
    }

    @Override
    public Page<Appointment> searchAppointments(Long doctorId, String searchKey, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("appointmentDate").descending().and(Sort.by("appointmentTime").descending()));

        if (searchKey.matches("^[0-9]{10}$")) {
            return appointmentRepository.findByDoctorIdAndPatientMobile(doctorId, searchKey, pageable);
        }
        else {
            return appointmentRepository.findByDoctorIdAndPatientNameContainingIgnoreCase(doctorId, searchKey, pageable);
        }
    }

    @Override
    public Page<Appointment> getUpcomingAppointmentsForPatient(Long patientId, int page, int size) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("appointmentDate").ascending()
                        .and(Sort.by("appointmentTime").ascending())
        );

        return appointmentRepository.findUpcomingAppointmentsForPatient(
                patientId,
                today,
                now,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED),
                pageable
        );
    }



}
