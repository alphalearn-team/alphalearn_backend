package com.example.demo.admin.lesson.dto;

import java.util.UUID;

import com.example.demo.lesson.LessonModerationStatus;

public record AdminLessonDetailDto(
    UUID contributorId,
    Integer lessonId,
    String lessonTitle,
    LessonModerationStatus lessonModerationStatus
) {}
