package com.vaidyalink.backend.controller;

import com.vaidyalink.backend.dto.SlotGenerateRequest;
import com.vaidyalink.backend.entity.DoctorSlot;
import com.vaidyalink.backend.service.DoctorSlotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorSlotController {

    private final DoctorSlotService doctorSlotService;

    public DoctorSlotController(DoctorSlotService doctorSlotService) {
        this.doctorSlotService = doctorSlotService;
    }

    @PostMapping("/{doctorId}/slots/generate")
    public ResponseEntity<?> generateSlots(
            @PathVariable Long doctorId,
            @RequestBody SlotGenerateRequest request) {

        List<DoctorSlot> generatedSlots = doctorSlotService.generateSlotsForDay(
                doctorId,
                request.getDate(),
                request.getShiftStartTime(),
                request.getShiftEndTime(),
                request.getDurationInMinutes()
        );

        return ResponseEntity.ok("Success! Generated " + generatedSlots.size() + " slots for Dr. ID: " + doctorId);
    }
}