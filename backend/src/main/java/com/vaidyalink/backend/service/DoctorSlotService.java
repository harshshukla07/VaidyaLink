package com.vaidyalink.backend.service;

import com.vaidyalink.backend.entity.DoctorSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DoctorSlotService {
    List<DoctorSlot> generateSlotsForDay(Long doctorId, LocalDate date, LocalTime shiftStartTime, LocalTime shiftEndTime, int durationInMinutes);
}