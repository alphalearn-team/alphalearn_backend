package com.example.demo.lesson.query;

import java.util.List;
import java.util.UUID;

import com.example.demo.lesson.enums.LessonModerationStatus;

public record LessonListCriteria(
        List<Integer> conceptIds,
        ConceptsMatchMode conceptsMatch,
        UUID contributorId,
        LessonModerationStatus status,
        LessonListAudience audience
) {
    public LessonListCriteria {
        conceptIds = conceptIds == null ? null : List.copyOf(conceptIds);
        conceptsMatch = conceptsMatch == null ? ConceptsMatchMode.ANY : conceptsMatch;
    }
}
