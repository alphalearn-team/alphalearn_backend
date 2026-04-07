package com.example.demo.quest.learner;

import java.util.UUID;

public record QuestChallengeTaggedFriendDto(
        UUID learnerPublicId,
        String learnerUsername
) {}
