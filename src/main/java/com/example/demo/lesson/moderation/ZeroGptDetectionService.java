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
            
            // FIX 1: Use the specific header name required by the API docs
            headers.set("ApiKey", apiKey);

            // FIX 2: Use "input_text" as the key, as shown in the documentation screenshot
            Map<String, Object> body = Map.of(
                "input_text", content
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // It is better to use String.class or JsonNode first to see exactly what the API returns
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.zerogpt.com/api/detect/detectText",
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
            
            String reason = String.format("ZeroGPT result: %.2f%% (Threshold: %.2f%%)", aiProbability, aiThreshold);

            return new ZeroGptDetectionResult(
                    aiProbability,
                    shouldReject,
                    reason,
                    responseBody
            );
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // IMPROVED DEBUGGING: Print the actual error body from the server
            System.err.println("API Error Response: " + e.getResponseBodyAsString());
            throw new RuntimeException("ZeroGPT API returned " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("ZeroGPT detection failed: " + e.getMessage(), e);
        }
    }

    private double parseAiProbability(Map<?, ?> responseBody) {
        // FIX 3: Based on your screenshot, 'fakePercentage' is the key used by this API
        Object value = responseBody.get("fakePercentage");
        
        if (value == null) {
            // Fallback to check if it's nested in a 'data' object (common in some API versions)
            Object data = responseBody.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                value = dataMap.get("fakePercentage");
            }
        }

        if (value == null) {
            throw new RuntimeException("Could not find 'fakePercentage' in response: " + responseBody);
        }
        
        return Double.parseDouble(value.toString());
    }
}
