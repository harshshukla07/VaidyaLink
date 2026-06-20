package com.vaidyalink.backend.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vaidyalink.backend.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
