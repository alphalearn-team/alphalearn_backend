package com.example.demo.me.weeklyquest;

import java.util.List;

public record QuestHistoryDto(
        List<QuestHistoryItemDto> items,
        int page,
        int size,
        boolean hasNext
) {
}
