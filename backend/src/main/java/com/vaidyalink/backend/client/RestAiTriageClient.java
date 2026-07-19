package com.vaidyalink.backend.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.vaidyalink.backend.dto.TriageRequest;
import com.vaidyalink.backend.dto.TriageResponse;
import com.vaidyalink.backend.exception.AiTriageException;

@Component
@ConditionalOnProperty(name = "ai.triage.stub-enabled", havingValue = "false")
public class RestAiTriageClient implements AiTriageClient {

    private final RestClient restClient;

    public RestAiTriageClient(RestClient aiTriageRestClient) {
        this.restClient = aiTriageRestClient;
    }

    @Override
    public TriageResponse triage(TriageRequest request) {
        try {
            TriageResponse response = restClient.post()
                    .uri("/api/ai/triage")
                    .body(request)
                    .retrieve()
                    .body(TriageResponse.class);

            if (response == null || response.getAiReply() == null) {
                throw new AiTriageException("AI triage service returned an empty response");
            }

            return response;
        } catch (RestClientException ex) {
            throw new AiTriageException("AI triage service is temporarily unavailable", ex);
        }
    }

    @Override
    public void warmup() {
        restClient.get()
                .uri("/health")
                .retrieve()
                .toBodilessEntity();
    }
}
