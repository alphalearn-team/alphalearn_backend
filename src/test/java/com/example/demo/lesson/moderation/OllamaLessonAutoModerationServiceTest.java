package com.example.demo.lesson.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonSection;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OllamaLessonAutoModerationServiceTest {

    @Mock
    private OllamaModerationService ollamaModerationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OllamaLessonAutoModerationService service;

    @BeforeEach
    void setUp() {
        service = new OllamaLessonAutoModerationService(ollamaModerationService);
    }

    @Test
    void moderateMapsRejectedToRejectDecision() throws Exception {
        Lesson lesson = lessonWithSection();
        when(ollamaModerationService.moderate(anyString()))
                .thenReturn(new ModerationResult("REJECTED", "hate speech"));

        LessonModerationResult result = service.moderate(lesson);

        assertThat(result.decision()).isEqualTo(LessonModerationDecision.REJECT);
        assertThat(result.reasons()).containsExactly("hate speech");
        assertThat(result.providerName()).isEqualTo("OLLAMA_PHI3");
        assertThat(result.completedAt()).isNotNull();

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(ollamaModerationService).moderate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("main-body");
        assertThat(promptCaptor.getValue()).contains("Section title");
        assertThat(promptCaptor.getValue()).contains("section-body");
    }

    @Test
    void moderateMapsApprovedAndUnknownToFlagDecision() throws Exception {
        Lesson lesson = lessonWithSection();
        when(ollamaModerationService.moderate(anyString()))
                .thenReturn(new ModerationResult("APPROVED", "ok"))
                .thenReturn(new ModerationResult("SOMETHING_ELSE", "unclear"));

        LessonModerationResult approvedResult = service.moderate(lesson);
        LessonModerationResult unknownResult = service.moderate(lesson);

        assertThat(approvedResult.decision()).isEqualTo(LessonModerationDecision.FLAG);
        assertThat(unknownResult.decision()).isEqualTo(LessonModerationDecision.FLAG);
    }

    private Lesson lessonWithSection() throws Exception {
        Lesson lesson = new Lesson();
        lesson.setContent(objectMapper.readTree("{\"text\":\"main-body\"}"));

        LessonSection section = new LessonSection();
        section.setTitle("Section title");
        section.setContent(objectMapper.readTree("{\"text\":\"section-body\"}"));

        lesson.setSections(List.of(section));
        return lesson;
    }
}
