package com.vaidyalink.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.vaidyalink.backend.client.AiTriageClient;
import com.vaidyalink.backend.dto.ChatResponse;
import com.vaidyalink.backend.dto.ChatSessionResponse;
import com.vaidyalink.backend.dto.MessageDTO;
import com.vaidyalink.backend.dto.SendMessageRequest;
import com.vaidyalink.backend.dto.TriageRequest;
import com.vaidyalink.backend.dto.TriageResponse;
import com.vaidyalink.backend.entity.ChatMessage;
import com.vaidyalink.backend.entity.ChatSession;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.entity.SessionStatus;
import com.vaidyalink.backend.repository.ChatMessageRepository;
import com.vaidyalink.backend.repository.ChatSessionRepository;
import com.vaidyalink.backend.repository.PatientRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class ChatServiceImpl implements ChatService {

    private final PatientRepository patientRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatPersistenceHelper chatPersistenceHelper;
    private final AiTriageClient aiTriageClient;

    public ChatServiceImpl(PatientRepository patientRepository,
                           ChatSessionRepository chatSessionRepository,
                           ChatMessageRepository chatMessageRepository,
                           ChatPersistenceHelper chatPersistenceHelper,
                           AiTriageClient aiTriageClient) {
        this.patientRepository = patientRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatPersistenceHelper = chatPersistenceHelper;
        this.aiTriageClient = aiTriageClient;
    }

    @Override
    @Transactional
    public ChatSessionResponse getOrCreateSession(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + patientId));

        ChatSession session = chatSessionRepository
                .findFirstByPatientIdAndStatusOrderByCreatedAtDesc(patientId, SessionStatus.ACTIVE)
                .orElse(null);

        if (session == null) {
            session = new ChatSession();
            session.setPatient(patient);
            session.setStatus(SessionStatus.ACTIVE);
            session = chatSessionRepository.save(session);
        }

        List<ChatMessage> chatMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        ChatSessionResponse response = new ChatSessionResponse();
        response.setSessionId(session.getId());

        List<MessageDTO> existingMessages = new ArrayList<>();
        for (ChatMessage message : chatMessages) {
            existingMessages.add(new MessageDTO(
                    message.getId(),
                    message.getSenderType().name(),
                    message.getMessageText()));
        }

        response.setExistingMessages(existingMessages);
        return response;
    }

    @Override
    public ChatResponse sendMessage(SendMessageRequest request, Long patientId) {
        List<MessageDTO> messages = chatPersistenceHelper.savePatientMessage(
                request.getSessionId(),
                patientId,
                request.getMessageText());

        TriageRequest triageRequest = TriageRequest.builder()
                .sessionId(request.getSessionId())
                .messages(messages)
                .build();

        TriageResponse triageResponse = aiTriageClient.triage(triageRequest);

        return chatPersistenceHelper.saveAiResponse(request.getSessionId(), triageResponse);
    }
}
