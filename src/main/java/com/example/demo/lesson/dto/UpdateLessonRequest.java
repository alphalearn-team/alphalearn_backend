package com.example.demo.lesson.dto;

public record UpdateLessonRequest(
        String title,
        Object content
) {}
