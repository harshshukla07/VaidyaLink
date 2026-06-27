package com.vaidyalink.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TriageResponse {
    @JsonProperty("ai_reply")
    private String aiReply;

    @JsonProperty("is_complete")
    private boolean triageComplete;

    @JsonProperty("recommended_specialty")
    private String recommendedSpecialty;
}
