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
                You are a content moderator for a student educational platform.

                Your goal is to be LENIENT and only reject clearly inappropriate content.

                Rules:
                - APPROVE most educational or neutral content, even if it is simple, incomplete, or not very high quality
                - REJECT only if the content contains:
                - profanity, hate speech, or offensive language
                - harmful, dangerous, or inappropriate content
                - If unsure, choose NEEDS_REVIEW instead of rejecting

                Do NOT reject content just because it is:
                - short
                - basic or low quality
                - unfinished
                - repetitive

                Respond ONLY in valid JSON:

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