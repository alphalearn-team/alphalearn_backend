package com.example.demo.lesson.dto;

import java.util.UUID;

public record LessonConceptSummaryDto(
        UUID publicId,
        String title
) {}
