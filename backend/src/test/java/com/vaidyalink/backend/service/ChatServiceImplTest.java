package com.vaidyalink.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaidyalink.backend.client.AiTriageClient;
import com.vaidyalink.backend.dto.ChatResponse;
import com.vaidyalink.backend.dto.MessageDTO;
import com.vaidyalink.backend.dto.SendMessageRequest;
import com.vaidyalink.backend.dto.TriageRequest;
import com.vaidyalink.backend.dto.TriageResponse;
import com.vaidyalink.backend.entity.ChatMessage;
import com.vaidyalink.backend.entity.ChatSession;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.entity.SenderType;
import com.vaidyalink.backend.entity.SessionStatus;
import com.vaidyalink.backend.repository.ChatMessageRepository;
import com.vaidyalink.backend.repository.ChatSessionRepository;
import com.vaidyalink.backend.repository.PatientRepository;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatPersistenceHelper chatPersistenceHelper;

    @Mock
    private AiTriageClient aiTriageClient;

    @Mock
    private DoctorService doctorService;

    @InjectMocks
    private ChatServiceImpl chatService;

    private Patient patient;
    private ChatSession session;
    private ChatMessage existingMessage;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);
        patient.setName("Alice");

        session = new ChatSession();
        session.setId(10L);
        session.setPatient(patient);
        session.setStatus(SessionStatus.ACTIVE);

        existingMessage = new ChatMessage();
        existingMessage.setId(100L);
        existingMessage.setSenderType(SenderType.PATIENT);
        existingMessage.setMessageText("Hello");
    }

    @Test
    void getOrCreateSession_ShouldReturnExistingSessionWithMessages() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(chatSessionRepository.findFirstByPatientIdAndStatusOrderByCreatedAtDesc(1L, SessionStatus.ACTIVE))
                .thenReturn(Optional.of(session));
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(existingMessage));

        var response = chatService.getOrCreateSession(1L);

        assertEquals(10L, response.getSessionId());
        assertEquals(1, response.getExistingMessages().size());
        assertEquals("PATIENT", response.getExistingMessages().get(0).getSenderType());
        verify(chatSessionRepository, never()).save(any());
    }

    @Test
    void getOrCreateSession_ShouldCreateNewSession_WhenNoneActive() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(chatSessionRepository.findFirstByPatientIdAndStatusOrderByCreatedAtDesc(1L, SessionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        ChatSession newSession = new ChatSession();
        newSession.setId(11L);
        newSession.setPatient(patient);
        newSession.setStatus(SessionStatus.ACTIVE);
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(newSession);
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(11L)).thenReturn(List.of());

        var response = chatService.getOrCreateSession(1L);

        assertEquals(11L, response.getSessionId());
        assertNotNull(response.getExistingMessages());
        assertEquals(0, response.getExistingMessages().size());
        verify(chatSessionRepository, times(1)).save(any(ChatSession.class));
    }

    @Test
    void sendMessage_ShouldOrchestratePersistenceAndAiClient() {
        SendMessageRequest request = SendMessageRequest.builder()
                .sessionId(10L)
                .messageText("I have a headache")
                .build();

        List<MessageDTO> history = List.of(
                new MessageDTO(1L, "PATIENT", "I have a headache"));

        TriageResponse triageResponse = TriageResponse.builder()
                .aiReply("Tell me more about your symptoms")
                .triageComplete(false)
                .recommendedSpecialty(null)
                .build();

        ChatResponse expected = new ChatResponse();
        expected.setAiReply("Tell me more about your symptoms");
        expected.setTriageComplete(false);

        List<String> specialties = List.of("Cardiologist", "Dermatology", "General Physician");

        when(chatPersistenceHelper.savePatientMessage(10L, 1L, "I have a headache")).thenReturn(history);
        when(doctorService.getDistinctSpecialities()).thenReturn(specialties);
        when(aiTriageClient.triage(any(TriageRequest.class))).thenReturn(triageResponse);
        when(chatPersistenceHelper.saveAiResponse(10L, triageResponse)).thenReturn(expected);

        ChatResponse result = chatService.sendMessage(request, 1L);

        assertEquals("Tell me more about your symptoms", result.getAiReply());
        assertFalse(result.isTriageComplete());
        verify(chatPersistenceHelper, times(1)).savePatientMessage(10L, 1L, "I have a headache");
        verify(doctorService, times(1)).getDistinctSpecialities();
        verify(aiTriageClient, times(1)).triage(argThat(req ->
                req.getAllowedSpecialties() != null
                        && req.getAllowedSpecialties().equals(specialties)));
        verify(chatPersistenceHelper, times(1)).saveAiResponse(10L, triageResponse);
    }
}
