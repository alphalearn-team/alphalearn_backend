package com.example.demo.lesson.moderation;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ZeroGptDetectionService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;
    private final double aiThreshold;

    public ZeroGptDetectionService(
            @org.springframework.beans.factory.annotation.Value("${zerogpt.api.key:}") String apiKey,
            @org.springframework.beans.factory.annotation.Value("${zerogpt.ai-threshold:80}") double aiThreshold
    ) {
        this.apiKey = apiKey;
        this.aiThreshold = aiThreshold;
    }

    public ZeroGptDetectionResult detect(String content) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new RuntimeException("ZeroGPT API key is not configured");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "text", content,
                    "include_sentence_analysis", false
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.zerogpt.org/api/v1/developer/ai-detection",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("ZeroGPT returned an empty response body");
            }

            double aiProbability = parseAiProbability(responseBody);
            boolean shouldReject = aiProbability >= aiThreshold;
            String reason = shouldReject
                    ? String.format("ZeroGPT AI probability %.2f%% exceeds threshold %.2f%%", aiProbability, aiThreshold)
                    : String.format("ZeroGPT AI probability %.2f%% is below threshold %.2f%%", aiProbability, aiThreshold);

            return new ZeroGptDetectionResult(
                    aiProbability,
                    shouldReject,
                    reason,
                    responseBody
            );
        } catch (Exception e) {
            throw new RuntimeException("ZeroGPT detection failed", e);
        }
    }

    private double parseAiProbability(Map<?, ?> responseBody) {
        Object directValue = responseBody.get("ai_percentage");
        if (directValue == null) {
            directValue = responseBody.get("fakePercentage");
        }
        if (directValue == null) {
            Object data = responseBody.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                directValue = dataMap.get("ai_percentage");
                if (directValue == null) {
                    directValue = dataMap.get("fakePercentage");
                }
            }
        }
        if (directValue == null) {
            throw new RuntimeException("ZeroGPT response missing ai percentage");
        }
        return Double.parseDouble(directValue.toString());
    }
}
