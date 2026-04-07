package com.example.demo.quest.weekly;

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
import com.example.demo.game.GameWeeklyFeaturedConceptService;
import com.example.demo.quest.weekly.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.quest.weekly.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.quest.weekly.enums.WeeklyQuestWeekStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class WeeklyQuestAutomationServiceTest {

    @Mock
    private WeeklyQuestWeekRepository weekRepository;
    @Mock
    private WeeklyQuestAssignmentRepository assignmentRepository;
    @Mock
    private GameWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;

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
        assertThat(week.getActivationSource()).isEqualTo(com.example.demo.quest.weekly.enums.WeeklyQuestActivationSource.FALLBACK);
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
