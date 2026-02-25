package com.example.demo.admin.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.demo.lesson.enums.LessonModerationStatus;

public record AdminLessonSummaryDto(
        Integer lessonId,
        String title,
        List<Integer> conceptIds,
        UUID contributorId,
        LessonModerationStatus lessonModerationStatus,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {}
