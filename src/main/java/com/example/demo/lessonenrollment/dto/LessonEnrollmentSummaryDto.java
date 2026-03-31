package com.example.demo.lessonenrollment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonEnrollmentSummaryDto", description = "Summary of a lesson enrollment for listing")
public record LessonEnrollmentSummaryDto(
    @Schema(description = "Public ID of the lesson")
    UUID lessonPublicId,

    @Schema(description = "Title of the lesson")
    String title,

    @Schema(description = "Whether the learner has completed the lesson")
    boolean completed,

    @Schema(description = "Timestamp of first completion")
    OffsetDateTime firstCompletedAt
) {}
