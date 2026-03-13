package com.example.demo.admin.weeklyquest;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.weeklyquest.QuestTemplate;
import com.example.demo.weeklyquest.enums.QuestSubmissionMode;

public record WeeklyQuestTemplateDto(
        UUID publicId,
        String code,
        String title,
        String instructionText,
        QuestSubmissionMode submissionMode,
        boolean active,
        OffsetDateTime createdAt
) {
    public static WeeklyQuestTemplateDto from(QuestTemplate template) {
        return new WeeklyQuestTemplateDto(
                template.getPublicId(),
                template.getCode(),
                template.getTitle(),
                template.getInstructionText(),
                template.getSubmissionMode(),
                template.isActive(),
                template.getCreatedAt()
        );
    }
}
