package com.vaidyalink.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.vaidyalink.backend.dto.TriageResponse;
import com.vaidyalink.backend.entity.ChatMessage;
import com.vaidyalink.backend.entity.ChatSession;
import com.vaidyalink.backend.entity.Patient;
import com.vaidyalink.backend.entity.SenderType;
import com.vaidyalink.backend.entity.SessionStatus;
import com.vaidyalink.backend.repository.ChatMessageRepository;
import com.vaidyalink.backend.repository.ChatSessionRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ChatPersistenceHelperTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatPersistenceHelper chatPersistenceHelper;

    private Patient patient;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);

        session = new ChatSession();
        session.setId(10L);
        session.setPatient(patient);
        session.setStatus(SessionStatus.ACTIVE);
    }

    @Test
    void savePatientMessage_ShouldSaveAndReturnHistory() {
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(buildMessage(1L, SenderType.PATIENT, "Headache")));

        var result = chatPersistenceHelper.savePatientMessage(10L, 1L, "Headache");

        assertEquals(1, result.size());
        assertEquals("PATIENT", result.get(0).getSenderType());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void savePatientMessage_ShouldThrow404_WhenSessionNotFound() {
        when(chatSessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> chatPersistenceHelper.savePatientMessage(99L, 1L, "Hi"));
    }

    @Test
    void savePatientMessage_ShouldThrow403_WhenPatientDoesNotOwnSession() {
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        assertThrows(AccessDeniedException.class,
                () -> chatPersistenceHelper.savePatientMessage(10L, 999L, "Hi"));

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void savePatientMessage_ShouldThrow400_WhenSessionNotActive() {
        session.setStatus(SessionStatus.ROUTED);
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> chatPersistenceHelper.savePatientMessage(10L, 1L, "Hi"));

        assertTrue(ex.getMessage().contains("no longer ACTIVE"));
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void saveAiResponse_ShouldSaveAiMessageAndReturnResponse() {
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        TriageResponse triageResponse = TriageResponse.builder()
                .aiReply("Please describe duration")
                .triageComplete(false)
                .recommendedSpecialty(null)
                .build();

        var result = chatPersistenceHelper.saveAiResponse(10L, triageResponse);

        assertEquals("Please describe duration", result.getAiReply());
        assertFalse(result.isTriageComplete());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(chatSessionRepository, never()).save(any());
    }

    @Test
    void saveAiResponse_ShouldMarkSessionRouted_WhenTriageComplete() {
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        TriageResponse triageResponse = TriageResponse.builder()
                .aiReply("Please see a cardiologist")
                .triageComplete(true)
                .recommendedSpecialty("Cardiology")
                .build();

        var result = chatPersistenceHelper.saveAiResponse(10L, triageResponse);

        assertTrue(result.isTriageComplete());
        assertEquals("Cardiology", result.getRecommendedSpecialty());

        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionRepository, times(1)).save(sessionCaptor.capture());
        assertEquals(SessionStatus.ROUTED, sessionCaptor.getValue().getStatus());
    }

    private ChatMessage buildMessage(Long id, SenderType senderType, String text) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setSenderType(senderType);
        message.setMessageText(text);
        return message;
    }
}
