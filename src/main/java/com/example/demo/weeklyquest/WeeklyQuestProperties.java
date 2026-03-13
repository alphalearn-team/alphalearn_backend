package com.example.demo.weeklyquest;

import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weekly-quest")
public record WeeklyQuestProperties(
        String zoneId,
        Fallback fallback,
        Reminder reminder,
        Activation activation
) {
    public record Fallback(
            UUID conceptPublicId,
            UUID questTemplatePublicId
    ) {}

    public record Reminder(
            int dailyHour
    ) {}

    public record Activation(
            int pollMinutes
    ) {}
}
