package com.example.demo.admin.lesson;

import java.util.UUID;

import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.dto.LessonAuthorDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminLessonDetail", description = "Admin response after moderation action")
public record AdminLessonDetailDto(
    @Schema(description = "Lesson author")
    LessonAuthorDto author,
    @Schema(description = "Lesson public UUID")
    UUID lessonPublicId,
    @Schema(description = "Lesson title")
    String lessonTitle,
    @Schema(description = "Updated lesson moderation status")
    LessonModerationStatus lessonModerationStatus
) {}
