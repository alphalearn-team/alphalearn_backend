package com.example.demo.admin.weeklyquest;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.concept.Concept;

public record WeeklyQuestConceptDto(
        UUID publicId,
        String title,
        String description,
        OffsetDateTime createdAt
) {
    public static WeeklyQuestConceptDto from(Concept concept) {
        return new WeeklyQuestConceptDto(
                concept.getPublicId(),
                concept.getTitle(),
                concept.getDescription(),
                concept.getCreatedAt()
        );
    }
}
