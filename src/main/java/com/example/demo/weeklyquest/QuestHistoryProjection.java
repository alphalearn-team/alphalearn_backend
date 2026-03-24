package com.example.demo.weeklyquest;

import com.example.demo.weeklyquest.enums.SubmissionVisibility;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface QuestHistoryProjection {

    UUID getSubmissionPublicId();

    UUID getLearnerPublicId();

    String getLearnerUsername();

    UUID getAssignmentPublicId();

    UUID getWeekPublicId();

    OffsetDateTime getWeekStartAt();

    UUID getConceptPublicId();

    String getConceptTitle();

    String getMediaPublicUrl();

    String getMediaContentType();

    String getOriginalFilename();

    String getCaption();

    OffsetDateTime getSubmittedAt();

    SubmissionVisibility getVisibility();
}
