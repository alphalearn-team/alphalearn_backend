package com.example.demo.admin.lesson;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.dto.LessonAuthorDto;

public record AdminLessonReviewDto(
        UUID lessonPublicId,
        String title,
        Object content,
        List<UUID> conceptPublicIds,
        LessonAuthorDto author,
        LessonModerationStatus lessonModerationStatus,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {}
