package com.vaidyalink.backend.client;

import com.vaidyalink.backend.dto.TriageRequest;
import com.vaidyalink.backend.dto.TriageResponse;

public interface AiTriageClient {
    TriageResponse triage(TriageRequest request);

    /**
     * Lightweight ping used to wake a sleeping AI instance (e.g. Render free tier).
     * Stub client is a no-op; REST client hits {@code GET /health}.
     */
    void warmup();
}
