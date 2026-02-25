package com.example.demo.lesson.dto;

import java.util.List;
import java.util.UUID;

public record CreateLessonRequest(
        String title,
        Object content,
        List<UUID> conceptPublicIds,
        Boolean submit
) {}
