package com.example.demo.lesson.dto;

import java.util.List;

public record CreateLessonRequest(
        String title,
        Object content,
        List<Integer> conceptIds,
        Boolean submit
) {}
