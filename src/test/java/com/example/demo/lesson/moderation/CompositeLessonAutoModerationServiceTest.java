package com.example.demo.lesson.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonModerationStatus;

@ExtendWith(MockitoExtension.class)
class CompositeLessonAutoModerationServiceTest {

    @Mock
    private ZeroGptDetectionService zeroGptDetectionService;

    @Mock
    private LessonAutoModerationService ollamaLessonAutoModerationService;

    private CompositeLessonAutoModerationService service;

    @BeforeEach
    void setUp() {
        service = new CompositeLessonAutoModerationService(
                zeroGptDetectionService,
                ollamaLessonAutoModerationService,
                new LessonModerationTextExtractor()
        );
    }

    @Test
    void moderateRejectsWhenDetectionRejectsEvenIfModerationApproves() {
        Lesson lesson = lesson();
        when(zeroGptDetectionService.detect(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                new ZeroGptDetectionResult(
                        95.0,
                        true,
                        "ZeroGPT AI probability 95.00% exceeds threshold 80.00%",
                        Map.of("ai_percentage", 95.0)
                )
        );
        when(ollamaLessonAutoModerationService.moderate(lesson)).thenReturn(new LessonModerationResult(
                LessonModerationDecision.APPROVE,
                List.of("Looks acceptable"),
                "OLLAMA_PHI3",
                Map.of("status", "APPROVED"),
                OffsetDateTime.now()
        ));

        LessonModerationResult result = service.moderate(lesson);

        assertThat(result.decision()).isEqualTo(LessonModerationDecision.REJECT);
        assertThat(result.reasons()).contains(
                "ZeroGPT AI probability 95.00% exceeds threshold 80.00%",
                "Ollama moderation: Looks acceptable"
        );
    }

    @Test
    void moderateFlagsWhenDetectionPassesAndModerationFlags() {
        Lesson lesson = lesson();
        when(zeroGptDetectionService.detect(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                new ZeroGptDetectionResult(
                        12.0,
                        false,
                        "ZeroGPT AI probability 12.00% is below threshold 80.00%",
                        Map.of("ai_percentage", 12.0)
                )
        );
        when(ollamaLessonAutoModerationService.moderate(lesson)).thenReturn(new LessonModerationResult(
                LessonModerationDecision.FLAG,
                List.of("Needs manual review"),
                "OLLAMA_PHI3",
                Map.of("status", "NEEDS_REVIEW"),
                OffsetDateTime.now()
        ));

        LessonModerationResult result = service.moderate(lesson);

        assertThat(result.decision()).isEqualTo(LessonModerationDecision.FLAG);
        assertThat(result.reasons()).contains(
                "ZeroGPT AI probability 12.00% is below threshold 80.00%",
                "Ollama moderation: Needs manual review"
        );
    }

    @Test
    void moderateThrowsWhenDetectionFails() {
        Lesson lesson = lesson();
        when(zeroGptDetectionService.detect(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("ZeroGPT detection failed"));
        when(ollamaLessonAutoModerationService.moderate(lesson)).thenReturn(new LessonModerationResult(
                LessonModerationDecision.APPROVE,
                List.of("Looks acceptable"),
                "OLLAMA_PHI3",
                null,
                OffsetDateTime.now()
        ));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.moderate(lesson));

        assertThat(ex.getMessage()).contains("ZeroGPT detection failed");
    }

    private Lesson lesson() {
        Lesson lesson = new Lesson();
        lesson.setTitle("Lesson title");
        lesson.setContent(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(Map.of("body", "hello")));
        lesson.setLessonModerationStatus(LessonModerationStatus.UNPUBLISHED);
        lesson.setCreatedAt(OffsetDateTime.now());
        return lesson;
    }
}
