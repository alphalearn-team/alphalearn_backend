package com.example.demo.me.weeklyquest;

import java.util.List;
import java.util.UUID;

public record QuestChallengeSubmissionCommand(
        String objectKey,
        String originalFilename,
        String caption,
        List<UUID> taggedFriendPublicIds
) {}
