package com.example.demo.lesson.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.lesson.Lesson;
import com.fasterxml.jackson.databind.ObjectMapper;

class KeywordLessonAutoModerationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private KeywordLessonAutoModerationService service;

    @BeforeEach
    void setUp() {
        service = new KeywordLessonAutoModerationService();
    }

    @Test
    void moderateReturnsRejectWhenRejectKeywordExists() throws Exception {
        Lesson lesson = lesson("How to avoid violence", "{\"text\":\"safe\"}");

        LessonModerationResult result = service.moderate(lesson);

        assertThat(result.decision()).isEqualTo(LessonModerationDecision.REJECT);
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("violence"));
        assertThat(result.providerName()).isEqualTo("LOCAL_KEYWORD_RULES");
    }

    @Test
    void moderateReturnsFlagWhenOnlyFlagKeywordExists() throws Exception {
        Lesson lesson = lesson("Study plan", "{\"text\":\"contains unsafe practice\"}");

        LessonModerationResult result = service.moderate(lesson);

        assertThat(result.decision()).isEqualTo(LessonModerationDecision.FLAG);
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("unsafe"));
    }

    @Test
    void moderateReturnsApproveWhenNoKeywordsFound() throws Exception {
        Lesson lesson = lesson("Introduction to fractions", "{\"text\":\"clean content\"}");

        LessonModerationResult result = service.moderate(lesson);

        assertThat(result.decision()).isEqualTo(LessonModerationDecision.APPROVE);
        assertThat(result.reasons()).contains("No blocked or flagged keywords detected");
    }

    private Lesson lesson(String title, String contentJson) throws Exception {
        Lesson lesson = new Lesson();
        lesson.setPublicId(UUID.randomUUID());
        lesson.setTitle(title);
        lesson.setContent(objectMapper.readTree(contentJson));
        return lesson;
    }
}
