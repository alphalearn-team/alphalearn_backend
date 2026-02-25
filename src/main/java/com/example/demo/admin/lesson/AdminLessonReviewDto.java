package com.example.demo.admin.lesson;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.demo.lesson.LessonModerationStatus;

public record AdminLessonReviewDto(
        Integer lessonId,
        String title,
        Object content,
        List<Integer> conceptIds,
        UUID contributorId,
        LessonModerationStatus lessonModerationStatus,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {}
