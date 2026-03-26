package com.example.demo.lesson.moderation;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.example.demo.lesson.Lesson;

@Primary
@Service
public class OllamaLessonAutoModerationService implements LessonAutoModerationService {

    private final OllamaModerationService ollamaService;

    public OllamaLessonAutoModerationService(OllamaModerationService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @Override
    public LessonModerationResult moderate(Lesson lesson) {
        System.out.println("🔥 AUTO MODERATION TRIGGERED");

        String content = extractContent(lesson);

        ModerationResult aiResult = ollamaService.moderate(content);

        return mapToLessonModerationResult(aiResult);
    }

    // 🔥 STEP 4 — Extract content (fits your system)
    private String extractContent(Lesson lesson) {
        StringBuilder sb = new StringBuilder();

        if (lesson.getContent() != null) {
            sb.append(lesson.getContent().toString()).append("\n");
        }

        if (lesson.getSections() != null) {
            lesson.getSections().forEach(section -> {
                if (section.getTitle() != null) {
                    sb.append(section.getTitle()).append("\n");
                }
                if (section.getContent() != null) {
                    sb.append(section.getContent().toString()).append("\n");
                }
            });
        }

        return sb.toString();
    }

    // 🔥 STEP 5 — Map AI → YOUR system
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
                "OLLAMA_PHI3", // provider name
                ai,            // raw response (store for debugging)
                OffsetDateTime.now()
        );
    }
}