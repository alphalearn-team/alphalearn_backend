package com.example.demo.lesson;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.lesson.moderation.LessonAutoModerationService;
import com.example.demo.lesson.moderation.LessonModerationDecision;
import com.example.demo.lesson.moderation.LessonModerationDecisionSource;
import com.example.demo.lesson.moderation.LessonModerationEventType;
import com.example.demo.lesson.moderation.LessonModerationRecord;
import com.example.demo.lesson.moderation.LessonModerationRecordRepository;
import com.example.demo.lesson.moderation.LessonModerationResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LessonModerationWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(LessonModerationWorkflowService.class);

    private final LessonRepository lessonRepository;
    private final LessonModerationRecordRepository lessonModerationRecordRepository;
    private final LessonAutoModerationService lessonAutoModerationService;
    private final ObjectMapper objectMapper;

    public LessonModerationWorkflowService(
            LessonRepository lessonRepository,
            LessonModerationRecordRepository lessonModerationRecordRepository,
            LessonAutoModerationService lessonAutoModerationService,
            ObjectMapper objectMapper
    ) {
        this.lessonRepository = lessonRepository;
        this.lessonModerationRecordRepository = lessonModerationRecordRepository;
        this.lessonAutoModerationService = lessonAutoModerationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Lesson submitForReview(Lesson lesson) {
        try {
            LessonModerationResult result = lessonAutoModerationService.moderate(lesson);
            LessonModerationStatus resultingStatus = toResultingStatus(result.decision());
            lesson.setLessonModerationStatus(resultingStatus);
            Lesson savedLesson = lessonRepository.save(lesson);
            lessonModerationRecordRepository.save(toAutoRecord(savedLesson, result, resultingStatus));
            return savedLesson;
        } catch (RuntimeException ex) {
            log.warn(
                    "Automatic moderation failed for lessonPublicId={} contributorId={} provider={} exceptionType={} message={}",
                    lesson.getPublicId(),
                    lesson.getContributor() == null ? null : lesson.getContributor().getContributorId(),
                    lessonAutoModerationService.getClass().getSimpleName(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage()
            );
            lesson.setLessonModerationStatus(LessonModerationStatus.PENDING);
            Lesson savedLesson = lessonRepository.save(lesson);
            lessonModerationRecordRepository.save(toAutoFailureRecord(savedLesson, ex));
            return savedLesson;
        }
    }

    @Transactional
    public Lesson unpublish(Lesson lesson) {
        lesson.setLessonModerationStatus(LessonModerationStatus.UNPUBLISHED);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public Lesson approve(Lesson lesson, UUID actorUserId) {
        requirePending(lesson, "approved");
        lesson.setLessonModerationStatus(LessonModerationStatus.APPROVED);
        Lesson savedLesson = lessonRepository.save(lesson);
        lessonModerationRecordRepository.save(toAdminRecord(
                savedLesson,
                LessonModerationEventType.ADMIN_APPROVED,
                actorUserId,
                null
        ));
        return savedLesson;
    }

    @Transactional
    public Lesson reject(Lesson lesson, String reviewNote, UUID actorUserId) {
        requirePending(lesson, "rejected");
        lesson.setLessonModerationStatus(LessonModerationStatus.REJECTED);
        Lesson savedLesson = lessonRepository.save(lesson);
        lessonModerationRecordRepository.save(toAdminRecord(
                savedLesson,
                LessonModerationEventType.ADMIN_REJECTED,
                actorUserId,
                reviewNote
        ));
        return savedLesson;
    }

    private LessonModerationStatus toResultingStatus(LessonModerationDecision decision) {
        return switch (decision) {
            case APPROVE -> LessonModerationStatus.APPROVED;
            case REJECT -> LessonModerationStatus.REJECTED;
            case FLAG -> LessonModerationStatus.PENDING;
        };
    }

    private LessonModerationRecord toAutoRecord(
            Lesson lesson,
            LessonModerationResult result,
            LessonModerationStatus resultingStatus
    ) {
        LessonModerationRecord record = new LessonModerationRecord();
        record.setLesson(lesson);
        record.setEventType(toAutoEventType(result.decision()));
        record.setDecisionSource(LessonModerationDecisionSource.AUTO);
        record.setResultingStatus(resultingStatus);
        record.setRecordedAt(result.completedAt() == null ? OffsetDateTime.now() : result.completedAt());
        record.setReasons(objectMapper.valueToTree(result.reasons() == null ? List.of() : result.reasons()));
        record.setFailureMessage(null);
        record.setRawResponse(result.rawResponse() == null ? null : objectMapper.valueToTree(result.rawResponse()));
        record.setReviewNote(null);
        record.setActorUserId(null);
        record.setProviderName(result.providerName());
        return record;
    }

    private LessonModerationRecord toAutoFailureRecord(Lesson lesson, RuntimeException ex) {
        LessonModerationRecord record = new LessonModerationRecord();
        record.setLesson(lesson);
        record.setEventType(LessonModerationEventType.AUTO_FAILED);
        record.setDecisionSource(LessonModerationDecisionSource.AUTO_FALLBACK);
        record.setResultingStatus(LessonModerationStatus.PENDING);
        record.setRecordedAt(OffsetDateTime.now());
        record.setReasons(objectMapper.valueToTree(List.of("Automatic moderation failed; sent for manual review")));
        record.setFailureMessage(ex.getMessage());
        record.setRawResponse(null);
        record.setReviewNote(null);
        record.setActorUserId(null);
        record.setProviderName(lessonAutoModerationService.getClass().getSimpleName());
        return record;
    }

    private LessonModerationEventType toAutoEventType(LessonModerationDecision decision) {
        return switch (decision) {
            case APPROVE -> LessonModerationEventType.AUTO_APPROVED;
            case REJECT -> LessonModerationEventType.AUTO_REJECTED;
            case FLAG -> LessonModerationEventType.AUTO_FLAGGED;
        };
    }

    private LessonModerationRecord toAdminRecord(
            Lesson lesson,
            LessonModerationEventType eventType,
            UUID actorUserId,
            String reviewNote
    ) {
        LessonModerationRecord record = new LessonModerationRecord();
        record.setLesson(lesson);
        record.setEventType(eventType);
        record.setDecisionSource(LessonModerationDecisionSource.ADMIN);
        record.setResultingStatus(lesson.getLessonModerationStatus());
        record.setRecordedAt(OffsetDateTime.now());
        record.setReasons(objectMapper.valueToTree(List.of()));
        record.setFailureMessage(null);
        record.setRawResponse(null);
        record.setReviewNote(reviewNote);
        record.setActorUserId(actorUserId);
        record.setProviderName(null);
        return record;
    }

    private void requirePending(Lesson lesson, String actionWord) {
        if (lesson.getLessonModerationStatus() != LessonModerationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only PENDING lessons can be " + actionWord + "."
            );
        }
    }
}
