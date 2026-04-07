package com.example.demo.quest.weekly;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weekly-quest")
public record WeeklyQuestProperties(
        String zoneId,
        Activation activation
) {
    public record Activation(
            int pollMinutes
    ) {}
}
