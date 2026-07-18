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
public class TriageRequest {
    private Long sessionId;
    private List<MessageDTO> messages;
    private List<String> allowedSpecialties;
}
