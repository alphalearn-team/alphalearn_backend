package com.example.demo.me.weeklyquest;

import com.example.demo.weeklyquest.enums.QuestSubmissionMode;

public record LearnerWeeklyQuestDetailsDto(
        String title,
        String instructionText,
        QuestSubmissionMode submissionMode
) {
    private static final String FIXED_TITLE = "Video + Caption";
    private static final String FIXED_INSTRUCTION_TEXT = "Record a short video and write a caption using this week's concept.";
    private static final QuestSubmissionMode FIXED_SUBMISSION_MODE = QuestSubmissionMode.VIDEO_WITH_CAPTION;

    public static LearnerWeeklyQuestDetailsDto fixed() {
        return new LearnerWeeklyQuestDetailsDto(
                FIXED_TITLE,
                FIXED_INSTRUCTION_TEXT,
                FIXED_SUBMISSION_MODE
        );
    }
}
