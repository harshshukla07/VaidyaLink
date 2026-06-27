package com.vaidyalink.backend.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaidyalink.backend.dto.MessageDTO;
import com.vaidyalink.backend.dto.TriageRequest;
import com.vaidyalink.backend.dto.TriageResponse;

class StubAiTriageClientTest {

    private final StubAiTriageClient client = new StubAiTriageClient();

    @Test
    void triage_ShouldReturnStubResponse() {
        TriageRequest request = TriageRequest.builder()
                .sessionId(1L)
                .messages(List.of(new MessageDTO(1L, "PATIENT", "I feel dizzy")))
                .build();

        TriageResponse response = client.triage(request);

        assertEquals("Thanks for sharing. Can you describe your symptoms in more detail?", response.getAiReply());
        assertFalse(response.isTriageComplete());
        assertNull(response.getRecommendedSpecialty());
    }
}
