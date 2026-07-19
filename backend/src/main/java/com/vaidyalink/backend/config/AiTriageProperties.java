package com.vaidyalink.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "ai.triage")
@Getter
@Setter
public class AiTriageProperties {
    private String baseUrl = "http://localhost:8000";
    private boolean stubEnabled = true;
    private String apiKey = "";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
}
