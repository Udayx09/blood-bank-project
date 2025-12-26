package com.bloodbank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    // Fallback models - if one quota is exhausted, try the next
    private static final String[] MODELS = {
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-pro"
    };

    private static final String SYSTEM_PROMPT = """
            You are a helpful blood donation assistant for a blood bank management system.
            Your role is to answer questions about:
            - Blood donation eligibility (age 18-65, weight >50kg, good health)
            - Blood type compatibility for donations and transfusions
            - Donation process and what to expect
            - Benefits of blood donation
            - Recovery after donation
            - Common myths about blood donation

            IMPORTANT: In this system, donors must wait 90 DAYS (3 months) between donations.
            This is our standard policy for donor safety and red blood cell recovery.

            Blood type compatibility quick guide:
            - O- is universal donor (can give to anyone)
            - AB+ is universal recipient (can receive from anyone)
            - Same type is always compatible
            - Rh- can donate to Rh+ but not vice versa

            Keep responses concise, friendly, and informative.
            If a question is outside blood donation topics, politely redirect to blood-related questions.
            Always encourage people to donate blood and save lives!
            """;

    @SuppressWarnings("unchecked")
    public String chat(String userMessage) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("Gemini API key not configured");
            return "Sorry, the AI assistant is not configured. Please contact support.";
        }

        // Build request body - embed system context in the message
        Map<String, Object> requestBody = new HashMap<>();
        String fullMessage = SYSTEM_PROMPT + "\n\nUser question: " + userMessage;

        Map<String, Object> userContent = new HashMap<>();
        userContent.put("role", "user");
        Map<String, String> userPart = new HashMap<>();
        userPart.put("text", fullMessage);
        userContent.put("parts", List.of(userPart));
        requestBody.put("contents", List.of(userContent));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 500);
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Try each model until one works
        for (String model : MODELS) {
            try {
                String url = GEMINI_BASE_URL + model + ":generateContent?key=" + apiKey;
                log.debug("Trying model: {}", model);

                @SuppressWarnings("rawtypes")
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            log.info("Successfully used model: {}", model);
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                log.warn("Model {} failed: {}", model,
                        errorMsg != null ? errorMsg.substring(0, Math.min(100, errorMsg.length())) : "unknown");
                // Continue to next model
            }
        }

        return "🚫 The AI assistant is temporarily busy. Please try again in a minute!";
    }
}
