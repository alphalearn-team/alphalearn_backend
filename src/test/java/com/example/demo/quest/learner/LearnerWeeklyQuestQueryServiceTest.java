package com.example.demo.quest.learner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.concept.Concept;
import com.example.demo.quest.weekly.WeeklyQuestAssignment;
import com.example.demo.quest.weekly.WeeklyQuestAssignmentRepository;
import com.example.demo.quest.weekly.WeeklyQuestCalendarService;
import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmissionRepository;
import com.example.demo.quest.weekly.WeeklyQuestWeek;
import com.example.demo.quest.weekly.WeeklyQuestWeekRepository;
import com.example.demo.quest.weekly.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.quest.weekly.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.quest.weekly.enums.WeeklyQuestWeekStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LearnerWeeklyQuestQueryServiceTest {

    @Mock
    private WeeklyQuestWeekRepository weeklyQuestWeekRepository;
    @Mock
    private WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;
    @Mock
    private WeeklyQuestChallengeSubmissionRepository weeklyQuestChallengeSubmissionRepository;

    private LearnerWeeklyQuestQueryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-22T01:00:00Z"), ZoneId.of("UTC"));
        WeeklyQuestCalendarService calendarService = new WeeklyQuestCalendarService(clock, ZoneId.of("Asia/Singapore"));
        service = new LearnerWeeklyQuestQueryService(
                weeklyQuestWeekRepository,
                weeklyQuestAssignmentRepository,
                weeklyQuestChallengeSubmissionRepository,
                calendarService
        );
    }

    @Test
    void returnsCurrentActiveWeeklyQuest() {
        WeeklyQuestWeek week = activeWeek(1L);
        WeeklyQuestAssignment assignment = activeAssignment(week, WeeklyQuestAssignmentSourceType.ADMIN);
        when(weeklyQuestWeekRepository.findByWeekStartAt(week.getWeekStartAt())).thenReturn(Optional.of(week));
        when(weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(1L)).thenReturn(Optional.of(assignment));

        Optional<LearnerCurrentWeeklyQuestDto> result = service.getCurrentWeeklyQuest(null);

        assertThat(result).isPresent();
        assertThat(result.get().weekStartAt()).isEqualTo(week.getWeekStartAt());
        assertThat(result.get().concept().title()).isEqualTo("fire");
        assertThat(result.get().quest().instructionText()).contains("this week's concept");
        assertThat(result.get().questChallengeSubmission()).isNull();
    }

    @Test
    void returnsFallbackGeneratedCurrentWeeklyQuest() {
        WeeklyQuestWeek week = activeWeek(2L);
        WeeklyQuestAssignment assignment = activeAssignment(week, WeeklyQuestAssignmentSourceType.FALLBACK);
        when(weeklyQuestWeekRepository.findByWeekStartAt(week.getWeekStartAt())).thenReturn(Optional.of(week));
        when(weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(2L)).thenReturn(Optional.of(assignment));

        Optional<LearnerCurrentWeeklyQuestDto> result = service.getCurrentWeeklyQuest(null);

        assertThat(result).isPresent();
        assertThat(result.get().quest().title()).isEqualTo("Video + Caption");
    }

    @Test
    void returnsEmptyWhenNoActiveCurrentQuestExists() {
        OffsetDateTime currentWeekStartAt = OffsetDateTime.parse("2026-03-22T00:00:00+08:00");
        when(weeklyQuestWeekRepository.findByWeekStartAt(currentWeekStartAt)).thenReturn(Optional.empty());

        Optional<LearnerCurrentWeeklyQuestDto> result = service.getCurrentWeeklyQuest(null);

        assertThat(result).isEmpty();
    }

    private WeeklyQuestWeek activeWeek(Long id) {
        WeeklyQuestWeek week = new WeeklyQuestWeek();
        ReflectionTestUtils.setField(week, "id", id);
        week.setWeekStartAt(OffsetDateTime.parse("2026-03-22T00:00:00+08:00"));
        week.setSetupDeadlineAt(OffsetDateTime.parse("2026-03-15T00:00:00+08:00"));
        week.setStatus(WeeklyQuestWeekStatus.ACTIVE);
        week.setCreatedAt(OffsetDateTime.parse("2026-03-15T00:00:00+08:00"));
        return week;
    }

    private WeeklyQuestAssignment activeAssignment(WeeklyQuestWeek week, WeeklyQuestAssignmentSourceType sourceType) {
        Concept concept = new Concept();
        ReflectionTestUtils.setField(concept, "publicId", UUID.randomUUID());
        concept.setTitle("fire");
        concept.setDescription("Describes something intense.");

        WeeklyQuestAssignment assignment = new WeeklyQuestAssignment();
        assignment.setWeek(week);
        assignment.setConcept(concept);
        assignment.setOfficial(true);
        assignment.setSourceType(sourceType);
        assignment.setStatus(WeeklyQuestAssignmentStatus.ACTIVE);
        return assignment;
    }
}
