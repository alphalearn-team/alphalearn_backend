package com.example.demo.quest.learner;

import java.util.List;

public record QuestHistoryDto(
        List<QuestHistoryItemDto> items,
        int page,
        int size,
        boolean hasNext
) {
}
