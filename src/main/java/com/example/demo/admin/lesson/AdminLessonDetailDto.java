package com.example.demo.admin.lesson;

import java.util.UUID;

import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.dto.LessonAuthorDto;

public record AdminLessonDetailDto(
    LessonAuthorDto author,
    UUID lessonPublicId,
    String lessonTitle,
    LessonModerationStatus lessonModerationStatus
) {}
