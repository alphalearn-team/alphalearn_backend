package com.example.demo.me.weeklyquest;

import com.example.demo.weeklyquest.QuestTemplate;
import com.example.demo.weeklyquest.enums.QuestSubmissionMode;

public record LearnerWeeklyQuestDetailsDto(
        String title,
        String instructionText,
        QuestSubmissionMode submissionMode
) {
    public static LearnerWeeklyQuestDetailsDto from(QuestTemplate template) {
        return new LearnerWeeklyQuestDetailsDto(
                template.getTitle(),
                template.getInstructionText(),
                template.getSubmissionMode()
        );
    }
}
