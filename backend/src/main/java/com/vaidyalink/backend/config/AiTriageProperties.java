package com.vaidyalink.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai.triage")
@Getter
@Setter
public class AiTriageProperties {
    private String baseUrl = "http://localhost:8000";
    private boolean stubEnabled = true;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
}
