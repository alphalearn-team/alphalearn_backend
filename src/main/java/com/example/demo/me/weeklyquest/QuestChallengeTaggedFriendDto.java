package com.example.demo.me.weeklyquest;

import java.util.UUID;

public record QuestChallengeTaggedFriendDto(
        UUID learnerPublicId,
        String learnerUsername
) {}
