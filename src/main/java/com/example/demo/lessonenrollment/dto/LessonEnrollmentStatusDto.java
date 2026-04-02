package com.example.demo.lessonenrollment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonEnrollmentStatusDto", description = "Response for checking enrollment status")
public record LessonEnrollmentStatusDto(
    @Schema(description = "Whether the user is enrolled")
    boolean enrolled
) {}
