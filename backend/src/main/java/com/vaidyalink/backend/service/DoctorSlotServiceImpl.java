package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.Doctor;
import com.vaidyalink.backend.entity.DoctorSlot;
import com.vaidyalink.backend.entity.SlotStatus;
import com.vaidyalink.backend.repository.DoctorRepository;
import com.vaidyalink.backend.repository.DoctorSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DoctorSlotServiceImpl implements DoctorSlotService {

    private final DoctorSlotRepository doctorSlotRepository;
    private final DoctorRepository doctorRepository;

    public DoctorSlotServiceImpl(DoctorSlotRepository doctorSlotRepository, DoctorRepository doctorRepository) {
        this.doctorSlotRepository = doctorSlotRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public List<DoctorSlot> generateSlotsForDay(Long doctorId, LocalDate date, LocalTime shiftStartTime, LocalTime shiftEndTime, int durationInMinutes) {

        if (date.equals(LocalDate.now()) && shiftStartTime.isBefore(LocalTime.now())) {
            throw new IllegalStateException("Slot can't be generated in past!");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));

        List<DoctorSlot> dailySlots = new ArrayList<>();
        LocalTime currentTime = shiftStartTime;

        while (!currentTime.plusMinutes(durationInMinutes).isAfter(shiftEndTime)) {

            DoctorSlot slot = new DoctorSlot();
            slot.setDoctor(doctor);
            slot.setSlotDate(date);
            slot.setStartTime(currentTime);
            slot.setEndTime(currentTime.plusMinutes(durationInMinutes));
            slot.setStatus(SlotStatus.AVAILABLE);

            dailySlots.add(slot);

            currentTime = currentTime.plusMinutes(durationInMinutes);
        }

        return doctorSlotRepository.saveAll(dailySlots);
    }
}