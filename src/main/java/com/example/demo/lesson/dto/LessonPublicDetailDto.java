package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LessonPublicDetailDto(
        UUID lessonPublicId,
        String title,
        Object content,
        List<UUID> conceptPublicIds,
        LessonAuthorDto author,
        OffsetDateTime createdAt
) implements LessonDetailView {}
