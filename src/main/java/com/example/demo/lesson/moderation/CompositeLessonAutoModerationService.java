package com.example.demo.lesson.moderation;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.demo.lesson.Lesson;

@Service("compositeLessonAutoModerationService")
public class CompositeLessonAutoModerationService implements LessonAutoModerationService {

    private final ZeroGptDetectionService zeroGptDetectionService;
    private final LessonAutoModerationService ollamaLessonAutoModerationService;
    private final LessonModerationTextExtractor lessonModerationTextExtractor;

    public CompositeLessonAutoModerationService(
            ZeroGptDetectionService zeroGptDetectionService,
            @Qualifier("ollamaLessonAutoModerationService") LessonAutoModerationService ollamaLessonAutoModerationService,
            LessonModerationTextExtractor lessonModerationTextExtractor
    ) {
        this.zeroGptDetectionService = zeroGptDetectionService;
        this.ollamaLessonAutoModerationService = ollamaLessonAutoModerationService;
        this.lessonModerationTextExtractor = lessonModerationTextExtractor;
    }

    @Override
    public LessonModerationResult moderate(Lesson lesson) {
        String lessonText = lessonModerationTextExtractor.extract(lesson);

        CompletableFuture<ZeroGptDetectionResult> detectionFuture = CompletableFuture.supplyAsync(
                () -> zeroGptDetectionService.detect(lessonText)
        );
        CompletableFuture<LessonModerationResult> moderationFuture = CompletableFuture.supplyAsync(
                () -> ollamaLessonAutoModerationService.moderate(lesson)
        );

        TaskOutcome<ZeroGptDetectionResult> detectionOutcome = captureOutcome(detectionFuture);
        TaskOutcome<LessonModerationResult> moderationOutcome = captureOutcome(moderationFuture);

        if (detectionOutcome.error() != null || moderationOutcome.error() != null) {
            throw new RuntimeException(buildFailureMessage(detectionOutcome, moderationOutcome));
        }

        ZeroGptDetectionResult detectionResult = detectionOutcome.value();
        LessonModerationResult moderationResult = moderationOutcome.value();

        List<String> reasons = new ArrayList<>();
        if (detectionResult.reason() != null && !detectionResult.reason().isBlank()) {
            reasons.add(detectionResult.reason());
        }
        reasons.addAll(formatModerationReasons(moderationResult));

        LessonModerationDecision decision = combineDecision(detectionResult, moderationResult);
        Map<String, Object> rawResponse = new LinkedHashMap<>();
        rawResponse.put("detection", detectionResult.rawResponse());
        rawResponse.put("moderation", moderationResult.rawResponse());

        return new LessonModerationResult(
                decision,
                reasons,
                "ZEROGPT_AND_OLLAMA",
                rawResponse,
                OffsetDateTime.now()
        );
    }

    private LessonModerationDecision combineDecision(
            ZeroGptDetectionResult detectionResult,
            LessonModerationResult moderationResult
    ) {
        if (detectionResult.shouldReject()) {
            return LessonModerationDecision.REJECT;
        }

        if (moderationResult.decision() == LessonModerationDecision.REJECT) {
            return LessonModerationDecision.REJECT;
        }

        if (moderationResult.decision() == LessonModerationDecision.FLAG) {
            return LessonModerationDecision.FLAG;
        }

        return LessonModerationDecision.APPROVE;
    }

    private List<String> formatModerationReasons(LessonModerationResult moderationResult) {
        if (moderationResult.reasons() == null || moderationResult.reasons().isEmpty()) {
            return List.of("Ollama moderation returned " + moderationResult.decision().name());
        }

        return moderationResult.reasons().stream()
                .map(reason -> "Ollama moderation: " + reason)
                .toList();
    }

    private String buildFailureMessage(
            TaskOutcome<ZeroGptDetectionResult> detectionOutcome,
            TaskOutcome<LessonModerationResult> moderationOutcome
    ) {
        List<String> reasons = new ArrayList<>();

        if (detectionOutcome.error() != null) {
            reasons.add("ZeroGPT detection failed: " + detectionOutcome.error().getMessage());
        } else if (detectionOutcome.value() != null && detectionOutcome.value().reason() != null) {
            reasons.add(detectionOutcome.value().reason());
        }

        if (moderationOutcome.error() != null) {
            reasons.add("Ollama moderation failed: " + moderationOutcome.error().getMessage());
        } else if (moderationOutcome.value() != null) {
            reasons.addAll(formatModerationReasons(moderationOutcome.value()));
        }

        return String.join(" | ", reasons);
    }

    private <T> TaskOutcome<T> captureOutcome(CompletableFuture<T> future) {
        try {
            return new TaskOutcome<>(future.join(), null);
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            return new TaskOutcome<>(null, cause);
        }
    }

    private record TaskOutcome<T>(T value, Throwable error) {
    }
}
