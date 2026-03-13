package com.example.demo.weeklyquest;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeeklyQuestActivationScheduler {

    private final WeeklyQuestAutomationService weeklyQuestAutomationService;

    public WeeklyQuestActivationScheduler(WeeklyQuestAutomationService weeklyQuestAutomationService) {
        this.weeklyQuestAutomationService = weeklyQuestAutomationService;
    }

    @Scheduled(fixedDelayString = "${weekly-quest.activation.poll-minutes:10}m")
    public void activateCurrentWeek() {
        weeklyQuestAutomationService.activateCurrentWeek();
    }
}
