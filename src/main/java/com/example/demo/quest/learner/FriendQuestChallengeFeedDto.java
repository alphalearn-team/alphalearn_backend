package com.example.demo.quest.learner;

import java.util.List;

public record FriendQuestChallengeFeedDto(
        List<FriendQuestChallengeFeedItemDto> items,
        int page,
        int size,
        boolean hasNext
) {
}
