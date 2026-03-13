package com.example.demo.weeklyquest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeeklyQuestCalendarServiceTest {

    private WeeklyQuestCalendarService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-19T01:00:00Z"), ZoneId.of("UTC"));
        service = new WeeklyQuestCalendarService(clock, ZoneId.of("Asia/Singapore"));
    }

    @Test
    void daysUntilSetupDeadlineReturnsThreeDays() {
        assertThat(service.daysUntilSetupDeadline(service.weekStartAt(LocalDate.parse("2026-03-29"))))
                .isEqualTo(3);
    }

    @Test
    void daysUntilSetupDeadlineReturnsOneDay() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-20T16:00:00Z"), ZoneId.of("UTC"));
        WeeklyQuestCalendarService oneDayService = new WeeklyQuestCalendarService(clock, ZoneId.of("Asia/Singapore"));

        assertThat(oneDayService.daysUntilSetupDeadline(oneDayService.weekStartAt(LocalDate.parse("2026-03-29"))))
                .isEqualTo(1);
    }

    @Test
    void daysUntilSetupDeadlineReturnsZeroWhenDeadlineIsToday() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-21T16:00:00Z"), ZoneId.of("UTC"));
        WeeklyQuestCalendarService todayService = new WeeklyQuestCalendarService(clock, ZoneId.of("Asia/Singapore"));

        assertThat(todayService.daysUntilSetupDeadline(todayService.weekStartAt(LocalDate.parse("2026-03-29"))))
                .isEqualTo(0);
    }
}
