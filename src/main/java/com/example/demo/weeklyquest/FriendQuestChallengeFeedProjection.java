package com.example.demo.weeklyquest;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface FriendQuestChallengeFeedProjection {

    UUID getSubmissionPublicId();

    UUID getLearnerPublicId();

    String getLearnerUsername();

    UUID getConceptPublicId();

    String getConceptTitle();

    UUID getAssignmentPublicId();

    String getMediaPublicUrl();

    String getMediaContentType();

    String getOriginalFilename();

    String getCaption();

    OffsetDateTime getSubmittedAt();
}
