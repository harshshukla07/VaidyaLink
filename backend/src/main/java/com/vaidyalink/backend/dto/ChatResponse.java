package com.vaidyalink.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatResponse {
    private String aiReply;
    private boolean triageComplete;
    private String recommendedSpecialty;
    private List<DoctorResponse> recommendedDoctors;
}
