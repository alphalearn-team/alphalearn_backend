package com.example.demo.lesson.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.demo.lesson.dto.response.LessonDetailView;

public record LessonPublicDetailDto(
        Integer lessonId,
        String title,
        Object content,
        List<Integer> conceptIds,
        UUID contributorId,
        OffsetDateTime createdAt
) implements LessonDetailView {}
