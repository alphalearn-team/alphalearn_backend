package com.example.demo.lesson.dto;

import java.util.UUID;

public record LessonAuthorDto(
        UUID publicId,
        String username
) {}
