package com.vaidyalink.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.vaidyalink.backend.dto.ChatResponse;
import com.vaidyalink.backend.dto.MessageDTO;
import com.vaidyalink.backend.dto.TriageResponse;
import com.vaidyalink.backend.entity.ChatMessage;
import com.vaidyalink.backend.entity.ChatSession;
import com.vaidyalink.backend.entity.SenderType;
import com.vaidyalink.backend.entity.SessionStatus;
import com.vaidyalink.backend.repository.ChatMessageRepository;
import com.vaidyalink.backend.repository.ChatSessionRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class ChatPersistenceHelper {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatPersistenceHelper(ChatSessionRepository chatSessionRepository,
                                 ChatMessageRepository chatMessageRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public List<MessageDTO> savePatientMessage(Long sessionId, Long patientId, String messageText) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found with id: " + sessionId));

        if (!session.getPatient().getId().equals(patientId)) {
            throw new AccessDeniedException("You are not allowed to send messages to this session");
        }

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Cannot send a message to a triage session that is no longer ACTIVE.");
        }

        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setSenderType(SenderType.PATIENT);
        message.setMessageText(messageText);
        chatMessageRepository.save(message);

        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(m -> new MessageDTO(m.getId(), m.getSenderType().name(), m.getMessageText()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatResponse saveAiResponse(Long sessionId, TriageResponse triageResponse) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found with id: " + sessionId));

        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSession(session);
        aiMessage.setSenderType(SenderType.AI_BOT);
        aiMessage.setMessageText(triageResponse.getAiReply());
        chatMessageRepository.save(aiMessage);

        if (triageResponse.isTriageComplete()) {
            session.setStatus(SessionStatus.ROUTED);
            chatSessionRepository.save(session);
        }

        ChatResponse response = new ChatResponse();
        response.setAiReply(triageResponse.getAiReply());
        response.setTriageComplete(triageResponse.isTriageComplete());
        response.setRecommendedSpecialty(triageResponse.getRecommendedSpecialty());
        return response;
    }
}
