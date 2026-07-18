package com.vaidyalink.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.vaidyalink.backend.client.AiTriageClient;
import com.vaidyalink.backend.dto.ChatResponse;
import com.vaidyalink.backend.dto.ChatSessionResponse;
import com.vaidyalink.backend.dto.DoctorResponse;
import com.vaidyalink.backend.dto.MessageDTO;
import com.vaidyalink.backend.dto.SendMessageRequest;
import com.vaidyalink.backend.dto.TriageRequest;
import com.vaidyalink.backend.dto.TriageResponse;
import com.vaidyalink.backend.entity.ChatMessage;
import com.vaidyalink.backend.entity.ChatSession;
import com.vaidyalink.backend.entity.Doctor;
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
    private final DoctorService doctorService;

    public ChatServiceImpl(PatientRepository patientRepository,
                           ChatSessionRepository chatSessionRepository,
                           ChatMessageRepository chatMessageRepository,
                           ChatPersistenceHelper chatPersistenceHelper,
                           AiTriageClient aiTriageClient,
                           DoctorService doctorService) {
        this.patientRepository = patientRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatPersistenceHelper = chatPersistenceHelper;
        this.aiTriageClient = aiTriageClient;
        this.doctorService = doctorService;
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

        List<String> allowedSpecialties = doctorService.getDistinctSpecialities();

        TriageRequest triageRequest = TriageRequest.builder()
                .sessionId(request.getSessionId())
                .messages(messages)
                .allowedSpecialties(allowedSpecialties)
                .build();

        TriageResponse triageResponse = aiTriageClient.triage(triageRequest);

        ChatResponse response = chatPersistenceHelper.saveAiResponse(request.getSessionId(), triageResponse);

        if(triageResponse.isTriageComplete()) {
            String speciality = triageResponse.getRecommendedSpecialty();

            if(speciality != null && !speciality.isBlank() && !speciality.equalsIgnoreCase("Emergency")) {
                List<Doctor> recommendedDoctors = doctorService.getDoctorsBySpeciality(speciality);
                List<DoctorResponse> doctorResponses = new ArrayList<>();
                for(Doctor doctor:recommendedDoctors) {
                    doctorResponses.add(new DoctorResponse(
                        doctor.getId(),
                        doctor.getName(),
                        doctor.getEmail(),
                        doctor.getSpeciality(),
                        doctor.getExperience()
                    ));
                }

                response.setRecommendedDoctors(doctorResponses);
            }
        }
        return response;
    }
}
