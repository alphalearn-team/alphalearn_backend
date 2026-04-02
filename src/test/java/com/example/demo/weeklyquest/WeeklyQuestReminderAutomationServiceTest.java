package com.example.demo.weeklyquest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.example.demo.concept.Concept;
import com.example.demo.game.imposter.ImposterWeeklyFeaturedConceptService;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;
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
    private ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;

    private WeeklyQuestAutomationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-15T01:00:00Z"), ZoneId.of("UTC"));
        WeeklyQuestCalendarService calendarService = new WeeklyQuestCalendarService(clock, ZoneId.of("Asia/Singapore"));
        service = new WeeklyQuestAutomationService(
                weekRepository,
                assignmentRepository,
                imposterWeeklyFeaturedConceptService,
                calendarService,
                clock
        );
    }

    @Test
    void emitsReminderWhenNextSchedulableWeekIsUnset(CapturedOutput output) {
        OffsetDateTime targetWeek = OffsetDateTime.parse("2026-03-29T00:00:00+08:00");
        when(weekRepository.findByWeekStartAt(targetWeek)).thenReturn(Optional.empty());

        service.sendMissingUpcomingWeekReminder();

        verify(weekRepository).findByWeekStartAt(targetWeek);
        assertThat(output.getOut())
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

        assertThat(output.getOut()).doesNotContain("is still unset");
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
                imposterWeeklyFeaturedConceptService,
                calendarService,
                afterCutoffClock
        );

        afterCutoffService.sendMissingUpcomingWeekReminder();

        verify(weekRepository, never()).findByWeekStartAt(OffsetDateTime.parse("2026-03-22T00:00:00+08:00"));
        assertThat(output.getOut()).doesNotContain("is still unset");
    }

    @Test
    void suppressesDuplicateReminderInSameProcessSameDay(CapturedOutput output) {
        OffsetDateTime targetWeek = OffsetDateTime.parse("2026-03-29T00:00:00+08:00");
        when(weekRepository.findByWeekStartAt(targetWeek)).thenReturn(Optional.empty());

        service.sendMissingUpcomingWeekReminder();
        service.sendMissingUpcomingWeekReminder();

        long reminderCount = output.getOut().lines().filter(line -> line.contains("is still unset")).count();
        assertThat(reminderCount).isEqualTo(1);
    }

    @Test
    void activatesCurrentWeekUsingMonthlyFeaturedConcept() {
        WeeklyQuestWeek week = currentScheduledWeek(12L);
        Concept concept = featuredConcept("Fractions");
        when(weekRepository.findByWeekStartAt(week.getWeekStartAt())).thenReturn(Optional.of(week));
        when(assignmentRepository.findByWeek_IdAndOfficialTrue(12L)).thenReturn(Optional.empty());
        when(weekRepository.findAllByOrderByWeekStartAtAsc()).thenReturn(List.of());
        when(imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept("2026-03"))
                .thenReturn(Optional.of(concept));
        when(assignmentRepository.save(any(WeeklyQuestAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(weekRepository.save(any(WeeklyQuestWeek.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.activateCurrentWeek();

        verify(imposterWeeklyFeaturedConceptService).resolveCurrentWeeklyFeaturedConcept("2026-03");
        verify(assignmentRepository).save(any(WeeklyQuestAssignment.class));
        assertThat(week.getStatus()).isEqualTo(WeeklyQuestWeekStatus.ACTIVE);
        assertThat(week.getActivationSource()).isEqualTo(com.example.demo.weeklyquest.enums.WeeklyQuestActivationSource.FALLBACK);
    }

    @Test
    void doesNotActivateWhenMonthlyFeaturedConceptIsMissing(CapturedOutput output) {
        WeeklyQuestWeek week = currentScheduledWeek(21L);
        when(weekRepository.findByWeekStartAt(week.getWeekStartAt())).thenReturn(Optional.of(week));
        when(assignmentRepository.findByWeek_IdAndOfficialTrue(21L)).thenReturn(Optional.empty());
        when(imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept("2026-03"))
                .thenReturn(Optional.empty());

        service.activateCurrentWeek();

        verify(assignmentRepository, never()).save(any(WeeklyQuestAssignment.class));
        assertThat(output.getOut()).contains("featured concept is not configured");
        assertThat(week.getStatus()).isEqualTo(WeeklyQuestWeekStatus.SCHEDULED);
    }

    @Test
    void doesNotOverrideExistingOfficialAssignment() {
        WeeklyQuestWeek week = activeWeek(33L);
        WeeklyQuestAssignment existingAssignment = new WeeklyQuestAssignment();
        existingAssignment.setWeek(week);
        existingAssignment.setConcept(featuredConcept("Ratios"));
        existingAssignment.setOfficial(true);
        existingAssignment.setSourceType(WeeklyQuestAssignmentSourceType.FALLBACK);
        existingAssignment.setStatus(WeeklyQuestAssignmentStatus.ACTIVE);

        when(weekRepository.findByWeekStartAt(week.getWeekStartAt())).thenReturn(Optional.of(week));
        when(assignmentRepository.findByWeek_IdAndOfficialTrue(33L)).thenReturn(Optional.of(existingAssignment));
        when(weekRepository.findAllByOrderByWeekStartAtAsc()).thenReturn(List.of());

        service.activateCurrentWeek();

        verify(imposterWeeklyFeaturedConceptService, never()).resolveCurrentWeeklyFeaturedConcept(any());
    }

    private WeeklyQuestWeek currentScheduledWeek(Long id) {
        WeeklyQuestWeek week = new WeeklyQuestWeek();
        ReflectionTestUtils.setField(week, "id", id);
        week.setWeekStartAt(OffsetDateTime.parse("2026-03-15T00:00:00+08:00"));
        week.setSetupDeadlineAt(OffsetDateTime.parse("2026-03-08T00:00:00+08:00"));
        week.setStatus(WeeklyQuestWeekStatus.SCHEDULED);
        week.setCreatedAt(OffsetDateTime.parse("2026-03-08T00:00:00+08:00"));
        return week;
    }

    private WeeklyQuestWeek activeWeek(Long id) {
        WeeklyQuestWeek week = currentScheduledWeek(id);
        week.setStatus(WeeklyQuestWeekStatus.ACTIVE);
        return week;
    }

    private Concept featuredConcept(String title) {
        Concept concept = new Concept();
        concept.setTitle(title);
        concept.setDescription("desc");
        return concept;
    }
}
