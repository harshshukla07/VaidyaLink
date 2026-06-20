package com.vaidyalink.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vaidyalink.backend.entity.ChatSession;
import com.vaidyalink.backend.entity.SessionStatus;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    
    Optional<ChatSession> findFirstByPatientIdAndStatusOrderByCreatedAtDesc(Long patientId, SessionStatus status);

}