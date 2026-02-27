package com.example.demo.lesson;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lesson moderation status lifecycle")
public enum LessonModerationStatus {
    PENDING,
    APPROVED,
    UNPUBLISHED,
    REJECTED
}
