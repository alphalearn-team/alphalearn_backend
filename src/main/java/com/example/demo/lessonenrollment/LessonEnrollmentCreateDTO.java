package com.example.demo.lessonenrollment;

import java.util.UUID;

public record LessonEnrollmentCreateDTO(
    UUID lessonPublicId
    // optional: Boolean completed  (only if you actually want clients to set this on create)
) {}