package com.vaidyalink.backend.service;

import com.vaidyalink.backend.dto.ChatResponse;
import com.vaidyalink.backend.dto.ChatSessionResponse;
import com.vaidyalink.backend.dto.SendMessageRequest;


public interface ChatService {

    ChatSessionResponse getOrCreateSession(Long patientId);
    
    ChatResponse sendMessage(SendMessageRequest request, Long patientId);
}