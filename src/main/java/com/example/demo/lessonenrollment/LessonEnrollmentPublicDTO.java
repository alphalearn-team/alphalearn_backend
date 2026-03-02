package com.example.demo.lessonenrollment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonEnrollmentPublicDTO(
    Integer enrollmentId,
    UUID learnerPublicId,
    UUID lessonPublicId,
    boolean completed,
    OffsetDateTime firstCompletedAt
) {}