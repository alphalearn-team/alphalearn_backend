package com.example.demo.lesson.moderation;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OllamaModerationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public OllamaModerationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ModerationResult moderate(String content) {
        try {
            String prompt = buildPrompt(content);

            Map<String, Object> request = Map.of(
                    "model", "phi3",
                    "prompt", prompt,
                    "stream", false
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "http://localhost:11434/api/generate",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return parseResponse(response.getBody());

        } catch (Exception e) {
            // fallback: treat as NEEDS_REVIEW
            return new ModerationResult("NEEDS_REVIEW", "AI failure: " + e.getMessage());
        }
    }

    private String buildPrompt(String content) {
        return """
                You are a strict content moderator for an educational platform.

                Evaluate the following lesson content based on:
                - Harmful content
                - Offensive language
                - Irrelevant or low-quality content

                Respond ONLY in JSON:

                {
                  "status": "APPROVED | REJECTED | NEEDS_REVIEW",
                  "reason": "short explanation"
                }

                Lesson:
                """ + content;
    }

    private ModerationResult parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String raw = root.get("response").asText();

        String json = extractJson(raw);

        JsonNode parsed = objectMapper.readTree(json);

        String status = parsed.get("status").asText();
        String reason = parsed.get("reason").asText();

        return new ModerationResult(status, reason);
    }

    // 🔥 VERY IMPORTANT (handles messy AI output)
    private String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start == -1 || end == -1) {
            throw new RuntimeException("Invalid AI response: " + text);
        }

        return text.substring(start, end + 1);
    }
}