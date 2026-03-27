package com.vaidyalink.backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotGenerateRequest {

    @NotNull(message = "Date cannot be null")
    @FutureOrPresent(message = "Cannot generate slots for past dates")
    private LocalDate date;

    @NotNull(message = "Shift start time is required")
    private LocalTime shiftStartTime;

    @NotNull(message = "Shift end time is required")
    private LocalTime shiftEndTime;

    @Min(value = 10, message = "Slot duration must be at least 10 minutes")
    private int durationInMinutes;
}