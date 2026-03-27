package com.vaidyalink.backend.repository;

import com.vaidyalink.backend.entity.DoctorSlot;
import com.vaidyalink.backend.entity.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, Long> {

    List<DoctorSlot> findByDoctorIdAndSlotDateAndStatus(Long doctorId, LocalDate date, SlotStatus status);

    Optional<DoctorSlot> findByDoctorIdAndSlotDateAndStartTime(Long doctorId, LocalDate date, LocalTime startTime);
}