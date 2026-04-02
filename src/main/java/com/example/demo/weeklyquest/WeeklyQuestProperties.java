package com.example.demo.weeklyquest;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weekly-quest")
public record WeeklyQuestProperties(
        String zoneId,
        Reminder reminder,
        Activation activation
) {
    public record Reminder(
            int dailyHour,
            int dailyMinute
    ) {}

    public record Activation(
            int pollMinutes
    ) {}
}
