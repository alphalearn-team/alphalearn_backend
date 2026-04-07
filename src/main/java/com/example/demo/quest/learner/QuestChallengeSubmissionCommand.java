package com.example.demo.quest.learner;

import java.util.List;
import java.util.UUID;

public record QuestChallengeSubmissionCommand(
        String objectKey,
        String originalFilename,
        String caption,
        List<UUID> taggedFriendPublicIds
) {}
