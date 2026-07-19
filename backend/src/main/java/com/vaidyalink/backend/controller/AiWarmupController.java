package com.vaidyalink.backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaidyalink.backend.client.AiTriageClient;

@RestController
@RequestMapping("/api/ai")
public class AiWarmupController {

    private final AiTriageClient aiTriageClient;

    public AiWarmupController(AiTriageClient aiTriageClient) {
        this.aiTriageClient = aiTriageClient;
    }

    /**
     * Authenticated keep-alive for the Python triage service.
     * Always returns 200 — even a failed/slow ping helps wake a cold Render instance.
     */
    @GetMapping("/warmup")
    public Map<String, String> warmup() {
        try {
            aiTriageClient.warmup();
            return Map.of("status", "UP");
        } catch (RuntimeException ex) {
            return Map.of("status", "WARMING");
        }
    }
}
