package com.vaidyalink.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Profile("dev")
@Service
public class EmailNotificationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // KAFKA LISTENER
    // This method is always listening in the background
    @KafkaListener(topics = "appointment-notifications", groupId = "vaidyalink-email-group")
    public void consumeAppointmentNotification(String jsonMessage) {
        try {
            // Step 1: Converting JSON Bytes into Java Map again (Deserialization)
            Map<String, Object> payload = objectMapper.readValue(jsonMessage, Map.class);

            // Step 2: Data extraction
            String patientEmail = (String) payload.get("patientEmail");
            String patientName = (String) payload.get("patientName");
            String doctorName = (String) payload.get("doctorName");
            String date = (String) payload.get("appointmentDate");
            String time = (String) payload.get("appointmentTime");

            // Step 3: for now printing mail login on console
            System.out.println("\n======================================================");
            System.out.println("🚀 ASYNC EMAIL WORKER TRIGGERED (Background Thread) 🚀");
            System.out.println("📧 Sending Email to: " + patientEmail);
            System.out.println("📌 Subject: Appointment Confirmed with " + doctorName);
            System.out.println("✉️ Body: Dear " + patientName + ", your appointment is confirmed for " + date + " at " + time + ".");
            System.out.println("======================================================\n");

        } catch (Exception e) {
            System.out.println("Error decoding Kafka message: " + e.getMessage());
        }
    }
}