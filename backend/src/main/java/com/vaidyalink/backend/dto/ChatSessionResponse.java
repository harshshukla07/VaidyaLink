package com.vaidyalink.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatSessionResponse {
    private Long sessionId;
    private List<MessageDTO> existingMessages;
}
