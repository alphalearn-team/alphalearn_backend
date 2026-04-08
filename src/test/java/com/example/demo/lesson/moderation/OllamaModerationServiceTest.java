package com.example.demo.lesson.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

class OllamaModerationServiceTest {

    private OllamaModerationService service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        service = new OllamaModerationService(new ObjectMapper());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void moderateParsesJsonEmbeddedInResponseText() {
        mockServer.expect(requestTo("http://20.239.71.5:11434/api/generate"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"response\":\"Some text before {\\\"status\\\":\\\"APPROVED\\\",\\\"reason\\\":\\\"looks safe\\\"} some text after\"}",
                        MediaType.APPLICATION_JSON
                ));

        ModerationResult result = service.moderate("lesson content");

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getReason()).isEqualTo("looks safe");
        mockServer.verify();
    }

    @Test
    void moderateFallsBackToNeedsReviewOnInvalidAiResponse() {
        mockServer.expect(requestTo("http://20.239.71.5:11434/api/generate"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"response\":\"no json payload\"}",
                        MediaType.APPLICATION_JSON
                ));

        ModerationResult result = service.moderate("lesson content");

        assertThat(result.getStatus()).isEqualTo("NEEDS_REVIEW");
        assertThat(result.getReason()).isEqualTo("Needs manual review");
        mockServer.verify();
    }
}
