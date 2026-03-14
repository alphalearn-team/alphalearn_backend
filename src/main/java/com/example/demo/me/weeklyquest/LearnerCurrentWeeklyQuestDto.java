package com.example.demo.me.weeklyquest;

import java.time.OffsetDateTime;

import com.example.demo.weeklyquest.WeeklyQuestAssignment;

public record LearnerCurrentWeeklyQuestDto(
        OffsetDateTime weekStartAt,
        LearnerWeeklyQuestConceptDto concept,
        LearnerWeeklyQuestDetailsDto quest
) {
    public static LearnerCurrentWeeklyQuestDto from(WeeklyQuestAssignment assignment) {
        return new LearnerCurrentWeeklyQuestDto(
                assignment.getWeek().getWeekStartAt(),
                LearnerWeeklyQuestConceptDto.from(assignment.getConcept()),
                LearnerWeeklyQuestDetailsDto.from(assignment.getQuestTemplate())
        );
    }
}
