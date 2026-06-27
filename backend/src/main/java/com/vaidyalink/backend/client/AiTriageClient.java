package com.vaidyalink.backend.client;

import com.vaidyalink.backend.dto.TriageRequest;
import com.vaidyalink.backend.dto.TriageResponse;

public interface AiTriageClient {
    TriageResponse triage(TriageRequest request);
}
