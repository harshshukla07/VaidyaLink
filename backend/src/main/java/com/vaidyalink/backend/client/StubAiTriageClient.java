package com.vaidyalink.backend.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.vaidyalink.backend.dto.TriageRequest;
import com.vaidyalink.backend.dto.TriageResponse;

@Component
@ConditionalOnProperty(name = "ai.triage.stub-enabled", havingValue = "true", matchIfMissing = true)
public class StubAiTriageClient implements AiTriageClient {

    @Override
    public TriageResponse triage(TriageRequest request) {
        return TriageResponse.builder()
                .aiReply("Thanks for sharing. Can you describe your symptoms in more detail?")
                .triageComplete(false)
                .recommendedSpecialty(null)
                .build();
    }
}
