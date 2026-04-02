package com.example.demo.lesson.moderation;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.lesson.Lesson;

@Service("ollamaLessonAutoModerationService")
public class OllamaLessonAutoModerationService implements LessonAutoModerationService {

    private final OllamaModerationService ollamaService;
    private final LessonModerationTextExtractor lessonModerationTextExtractor;

    public OllamaLessonAutoModerationService(
            OllamaModerationService ollamaService,
            LessonModerationTextExtractor lessonModerationTextExtractor
    ) {
        this.ollamaService = ollamaService;
        this.lessonModerationTextExtractor = lessonModerationTextExtractor;
    }

    @Override
    public LessonModerationResult moderate(Lesson lesson) {
        String content = lessonModerationTextExtractor.extract(lesson);
        ModerationResult aiResult = ollamaService.moderate(content);
        return mapToLessonModerationResult(aiResult);
    }

    private LessonModerationResult mapToLessonModerationResult(ModerationResult ai) {
        LessonModerationDecision decision;

        switch (ai.getStatus().toUpperCase()) {
            case "REJECTED" -> decision = LessonModerationDecision.REJECT;
            case "APPROVED", "NEEDS_REVIEW" -> decision = LessonModerationDecision.FLAG;
            default -> decision = LessonModerationDecision.FLAG;
        }

        return new LessonModerationResult(
                decision,
                List.of(ai.getReason()),
                "OLLAMA_PHI3",
                ai,
                OffsetDateTime.now()
        );
    }
}
