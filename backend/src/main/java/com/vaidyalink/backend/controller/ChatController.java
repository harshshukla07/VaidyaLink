package com.vaidyalink.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaidyalink.backend.dto.ChatResponse;
import com.vaidyalink.backend.dto.ChatSessionResponse;
import com.vaidyalink.backend.dto.SendMessageRequest;
import com.vaidyalink.backend.repository.PatientRepository;
import com.vaidyalink.backend.service.ChatService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final PatientRepository patientRepository;

    public ChatController(ChatService chatService, PatientRepository patientRepository) {
        this.chatService = chatService;
        this.patientRepository = patientRepository;
    }

    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/session")
    public ResponseEntity<ChatSessionResponse> getOrCreateSession(Authentication authentication) {
        
        Long patientId = resolveAuthenticatedPatientId(authentication);
        ChatSessionResponse response = chatService.getOrCreateSession(patientId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/send")
    public ResponseEntity<ChatResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        Long patientId = resolveAuthenticatedPatientId(authentication);
        ChatResponse response = chatService.sendMessage(request,patientId);
       
        return ResponseEntity.ok(response);
    }

    private Long resolveAuthenticatedPatientId(Authentication authentication) {
        Long patientId = patientRepository.findByEmail(authentication.getName()).orElseThrow(() -> new EntityNotFoundException("Patient not found for authenticated user")).getId();
        return patientId;
    }
}
