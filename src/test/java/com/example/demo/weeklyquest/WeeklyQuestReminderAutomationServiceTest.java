package com.example.demo.weeklyquest;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.concept.ConceptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class WeeklyQuestReminderAutomationServiceTest {

    @Mock
    private WeeklyQuestWeekRepository weekRepository;
    @Mock
    private WeeklyQuestAssignmentRepository assignmentRepository;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private QuestTemplateRepository templateRepository;

    private WeeklyQuestAutomationService service;
    private WeeklyQuestProperties properties;

    @BeforeEach
    void setUp() {
        properties = new WeeklyQuestProperties(
                "Asia/Singapore",
                new WeeklyQuestProperties.Fallback(UUID.randomUUID(), UUID.randomUUID()),
                new WeeklyQuestProperties.Reminder(9),
                new WeeklyQuestProperties.Activation(10)
        );
        Clock clock = Clock.fixed(Instant.parse("2026-03-15T01:00:00Z"), ZoneId.of("UTC"));
        WeeklyQuestCalendarService calendarService = new WeeklyQuestCalendarService(clock, ZoneId.of("Asia/Singapore"));
        service = new WeeklyQuestAutomationService(
                weekRepository,
                assignmentRepository,
                conceptRepository,
                templateRepository,
                calendarService,
                properties,
                clock
        );
    }

    @Test
    void emitsReminderWhenNextSchedulableWeekIsUnset(CapturedOutput output) {
        OffsetDateTime targetWeek = OffsetDateTime.parse("2026-03-29T00:00:00+08:00");
        when(weekRepository.findByWeekStartAt(targetWeek)).thenReturn(Optional.empty());

        service.sendMissingUpcomingWeekReminder();

        verify(weekRepository).findByWeekStartAt(targetWeek);
        org.assertj.core.api.Assertions.assertThat(output.getOut())
                .contains("is still unset")
                .contains("7 days left");
    }

    @Test
    void doesNotEmitReminderWhenOfficialAssignmentExists(CapturedOutput output) {
        OffsetDateTime targetWeek = OffsetDateTime.parse("2026-03-29T00:00:00+08:00");
        WeeklyQuestWeek week = new WeeklyQuestWeek();
        ReflectionTestUtils.setField(week, "id", 7L);
        week.setWeekStartAt(targetWeek);
        week.setSetupDeadlineAt(targetWeek.minusDays(7));
        when(weekRepository.findByWeekStartAt(targetWeek)).thenReturn(Optional.of(week));
        when(assignmentRepository.findByWeek_IdAndOfficialTrue(7L)).thenReturn(Optional.of(new WeeklyQuestAssignment()));

        service.sendMissingUpcomingWeekReminder();

        org.assertj.core.api.Assertions.assertThat(output.getOut()).doesNotContain("is still unset");
    }

    @Test
    void doesNotEmitReminderAfterCutoff(CapturedOutput output) {
        Clock afterCutoffClock = Clock.fixed(Instant.parse("2026-03-22T01:00:00Z"), ZoneId.of("UTC"));
        WeeklyQuestCalendarService calendarService = new WeeklyQuestCalendarService(afterCutoffClock, ZoneId.of("Asia/Singapore")) {
            @Override
            public OffsetDateTime nextSchedulableWeekStartAt() {
                return OffsetDateTime.parse("2026-03-22T00:00:00+08:00");
            }
        };
        WeeklyQuestAutomationService afterCutoffService = new WeeklyQuestAutomationService(
                weekRepository,
                assignmentRepository,
                conceptRepository,
                templateRepository,
                calendarService,
                properties,
                afterCutoffClock
        );

        afterCutoffService.sendMissingUpcomingWeekReminder();

        verify(weekRepository, never()).findByWeekStartAt(OffsetDateTime.parse("2026-03-22T00:00:00+08:00"));
        org.assertj.core.api.Assertions.assertThat(output.getOut()).doesNotContain("is still unset");
    }

    @Test
    void suppressesDuplicateReminderInSameProcessSameDay(CapturedOutput output) {
        OffsetDateTime targetWeek = OffsetDateTime.parse("2026-03-29T00:00:00+08:00");
        when(weekRepository.findByWeekStartAt(targetWeek)).thenReturn(Optional.empty());

        service.sendMissingUpcomingWeekReminder();
        service.sendMissingUpcomingWeekReminder();

        long reminderCount = output.getOut().lines().filter(line -> line.contains("is still unset")).count();
        org.assertj.core.api.Assertions.assertThat(reminderCount).isEqualTo(1);
    }
}
