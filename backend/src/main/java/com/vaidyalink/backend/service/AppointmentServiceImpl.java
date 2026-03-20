package com.vaidyalink.backend.service;

import com.vaidyalink.backend.dto.AppointmentRequest;
import com.vaidyalink.backend.entity.Appointment;
import com.vaidyalink.backend.entity.AppointmentStatus;
import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.repository.AppointmentRepository;
import com.vaidyalink.backend.repository.DoctorRepository;
import com.vaidyalink.backend.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService{
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, PatientRepository patientRepository, DoctorRepository doctorRepository){
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }


    @Override
    public Appointment bookAppointment(AppointmentRequest request){

        LocalTime cleanTime = request.getAppointmentTime().truncatedTo(ChronoUnit.MINUTES);

        request.setAppointmentTime(cleanTime);

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (request.getAppointmentDate().equals(today)) {
            if (request.getAppointmentTime().isBefore(now)) {
                throw new IllegalStateException("You cannot book a past time for today's date.");
            }
        }

        int minutes = request.getAppointmentTime().getMinute();
        if (minutes != 0 && minutes != 20 && minutes != 40) {
            throw new IllegalStateException("Invalid slot! Appointments can only be booked at :00 or :20 or :40 minutes (e.g., 10:00, 10:20, 10:40).");
        }

        //Fetching patient by id
        Patient patient = patientRepository.findById(request.getPatientId()).orElseThrow(()-> new RuntimeException("Patient Not Found with id: "+request.getPatientId()));
        //Fetching Doctor by id
        Doctor doctor = doctorRepository.findById(request.getDoctorId()).orElseThrow(()-> new RuntimeException("Doctor Not Found with id: "+request.getDoctorId()));
        //Create new Appointment Object
        Appointment appointment = new Appointment();

        boolean isSlotTaken = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(request.getDoctorId(), request.getAppointmentDate(), request.getAppointmentTime(), AppointmentStatus.CANCELLED);
        if(isSlotTaken){
            throw new IllegalStateException("Sorry, This slot for Dr." + doctor.getName() + " is already booked.");
        }

        //Set data using setters
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setStatus(AppointmentStatus.PENDING);

        return appointmentRepository.save(appointment);
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

        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Doctor not found with ID: " + doctorId));

        List<LocalTime> availableSlots = new ArrayList<>();

        // SHIFT 1: Morning OPD (10:00 AM to 1:00 PM)
        LocalTime morningStart = LocalTime.of(10, 0);
        LocalTime morningEnd = LocalTime.of(13, 0);

        while (morningStart.isBefore(morningEnd)) {
            availableSlots.add(morningStart);
            morningStart = morningStart.plusMinutes(20);
        }

        // SHIFT 2: Evening OPD (6:00 PM to 9:00 PM)
        LocalTime eveningStart = LocalTime.of(18, 0);
        LocalTime eveningEnd = LocalTime.of(21, 0);

        while (eveningStart.isBefore(eveningEnd)) {
            availableSlots.add(eveningStart);
            eveningStart = eveningStart.plusMinutes(20);
        }

        List<Appointment> bookedAppointments = appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusNot(doctorId, date, AppointmentStatus.CANCELLED);

        List<LocalTime> bookedTimes = bookedAppointments.stream()
                .map(Appointment::getAppointmentTime)
                .toList();

        availableSlots.removeAll(bookedTimes);

        if (date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            availableSlots.removeIf(slot -> slot.isBefore(now));
        }

        return availableSlots;
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
