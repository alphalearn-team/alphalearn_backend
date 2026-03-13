package com.example.demo.weeklyquest;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeeklyQuestReminderScheduler {

    private final WeeklyQuestAutomationService weeklyQuestAutomationService;

    public WeeklyQuestReminderScheduler(WeeklyQuestAutomationService weeklyQuestAutomationService) {
        this.weeklyQuestAutomationService = weeklyQuestAutomationService;
    }

    @Scheduled(
            cron = "0 0 ${weekly-quest.reminder.daily-hour:9} * * *",
            zone = "${weekly-quest.zone-id:UTC}"
    )
    public void sendReminderForUnsetUpcomingWeek() {
        weeklyQuestAutomationService.sendMissingUpcomingWeekReminder();
    }
}
