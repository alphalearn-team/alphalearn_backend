package com.example.demo.me.weeklyquest;

import java.util.UUID;

import com.example.demo.concept.Concept;

public record LearnerWeeklyQuestConceptDto(
        UUID publicId,
        String title,
        String description
) {
    public static LearnerWeeklyQuestConceptDto from(Concept concept) {
        return new LearnerWeeklyQuestConceptDto(
                concept.getPublicId(),
                concept.getTitle(),
                concept.getDescription()
        );
    }
}
