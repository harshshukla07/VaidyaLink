package com.vaidyalink.backend.dto;

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
}
