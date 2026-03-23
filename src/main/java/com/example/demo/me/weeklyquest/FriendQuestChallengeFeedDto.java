package com.example.demo.me.weeklyquest;

import java.util.List;

public record FriendQuestChallengeFeedDto(
        List<FriendQuestChallengeFeedItemDto> items,
        int page,
        int size,
        boolean hasNext
) {
}
