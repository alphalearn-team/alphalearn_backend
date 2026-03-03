package com.example.demo.lesson.moderation;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.lesson.Lesson;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class KeywordLessonAutoModerationService implements LessonAutoModerationService {

    private static final String PROVIDER_NAME = "LOCAL_KEYWORD_RULES";

    private static final List<String> REJECT_KEYWORDS = List.of(
            "hate",
            "violence",
            "kill"
    );

    private static final List<String> FLAG_KEYWORDS = List.of(
            "cheat",
            "unsafe",
            "nsfw"
    );

    @Override
    public LessonModerationResult moderate(Lesson lesson) {
        String normalizedText = normalizeLessonText(lesson);
        List<String> rejectReasons = matchingReasons(normalizedText, REJECT_KEYWORDS, "Rejected keyword detected");
        if (!rejectReasons.isEmpty()) {
            return buildResult(LessonModerationDecision.REJECT, rejectReasons, lesson);
        }

        List<String> flagReasons = matchingReasons(normalizedText, FLAG_KEYWORDS, "Flagged keyword detected");
        if (!flagReasons.isEmpty()) {
            return buildResult(LessonModerationDecision.FLAG, flagReasons, lesson);
        }

        return buildResult(
                LessonModerationDecision.APPROVE,
                List.of("No blocked or flagged keywords detected"),
                lesson
        );
    }

    private LessonModerationResult buildResult(
            LessonModerationDecision decision,
            List<String> reasons,
            Lesson lesson
    ) {
        Map<String, Object> rawResponse = new LinkedHashMap<>();
        rawResponse.put("provider", PROVIDER_NAME);
        rawResponse.put("decision", decision.name());
        rawResponse.put("lessonPublicId", lesson.getPublicId());
        rawResponse.put("reasons", reasons);

        return new LessonModerationResult(
                decision,
                reasons,
                PROVIDER_NAME,
                rawResponse,
                OffsetDateTime.now()
        );
    }

    private List<String> matchingReasons(String normalizedText, List<String> keywords, String prefix) {
        List<String> reasons = new ArrayList<>();
        for (String keyword : keywords) {
            if (normalizedText.contains(keyword)) {
                reasons.add(prefix + ": " + keyword);
            }
        }
        return reasons;
    }

    private String normalizeLessonText(Lesson lesson) {
        StringBuilder text = new StringBuilder();
        if (lesson.getTitle() != null) {
            text.append(lesson.getTitle()).append(' ');
        }

        JsonNode content = lesson.getContent();
        if (content != null) {
            text.append(content.toString());
        }

        return text.toString().toLowerCase(Locale.ROOT);
    }
}
