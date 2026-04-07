package com.example.demo.lesson.enrollment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonProgressDto", description = "Progress summary for an enrolled lesson")
public record LessonProgressDto(
    @Schema(description = "Public ID of the lesson")
    UUID lessonPublicId,

    @Schema(description = "Title of the lesson")
    String title,

    @Schema(description = "Whether the learner has completed the lesson (all quizzes passed at full marks)")
    boolean completed,

    @Schema(description = "Timestamp of first completion")
    OffsetDateTime firstCompletedAt,

    @Schema(description = "Total number of quizzes in the lesson")
    int totalQuizzes,

    @Schema(description = "Number of quizzes the learner has passed at full marks")
    int passedQuizzes
) {}
