package com.example.demo.quest.learner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.learner.Learner;
import com.example.demo.storage.r2.QuestChallengeStorageService;
import com.example.demo.quest.weekly.WeeklyQuestAssignment;
import com.example.demo.quest.weekly.WeeklyQuestAssignmentRepository;
import com.example.demo.quest.weekly.WeeklyQuestCalendarService;
import com.example.demo.quest.weekly.WeeklyQuestWeek;
import com.example.demo.quest.weekly.WeeklyQuestWeekRepository;
import com.example.demo.quest.weekly.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.quest.weekly.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.quest.weekly.enums.WeeklyQuestWeekStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LearnerQuestChallengeUploadServiceTest {

    @Mock
    private WeeklyQuestWeekRepository weeklyQuestWeekRepository;
    @Mock
    private WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;
    @Mock
    private QuestChallengeStorageService questChallengeStorageService;

    private LearnerQuestChallengeUploadService service;
    private SupabaseAuthUser learnerUser;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-22T01:00:00Z"), ZoneId.of("UTC"));
        WeeklyQuestCalendarService calendarService = new WeeklyQuestCalendarService(clock, ZoneId.of("Asia/Singapore"));
        service = new LearnerQuestChallengeUploadService(
                weeklyQuestWeekRepository,
                weeklyQuestAssignmentRepository,
                calendarService,
                questChallengeStorageService
        );
        UUID learnerId = UUID.randomUUID();
        learnerUser = new SupabaseAuthUser(
                learnerId,
                new Learner(learnerId, UUID.randomUUID(), "learner", OffsetDateTime.parse("2026-03-01T00:00:00Z"), (short) 0),
                null
        );
    }

    @Test
    void createsUploadInstructionsForActiveQuestChallenge() {
        WeeklyQuestWeek week = activeWeek(1L);
        WeeklyQuestAssignment assignment = activeAssignment(week);
        QuestChallengeUploadRequest request = new QuestChallengeUploadRequest("evidence.mp4", "video/mp4", 1024L);

        when(weeklyQuestWeekRepository.findByWeekStartAt(week.getWeekStartAt())).thenReturn(Optional.of(week));
        when(weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(1L)).thenReturn(Optional.of(assignment));
        when(questChallengeStorageService.maxUploadSizeBytes()).thenReturn(52428800L);
        when(questChallengeStorageService.generatePresignedUpload(
                assignment.getPublicId(),
                learnerUser.userId(),
                "evidence.mp4",
                "video/mp4"
        )).thenReturn(new QuestChallengeStorageService.PresignedUpload(
                "quest-challenges/assignment/learner/evidence.mp4",
                "https://pub.example/quest-challenges/assignment/learner/evidence.mp4",
                "https://signed.example",
                OffsetDateTime.parse("2026-03-22T01:15:00Z"),
                Map.of("Content-Type", "video/mp4")
        ));

        QuestChallengeUploadResponse response = service.createUploadInstruction(request, learnerUser);

        assertThat(response.objectKey()).isEqualTo("quest-challenges/assignment/learner/evidence.mp4");
        assertThat(response.uploadUrl()).isEqualTo("https://signed.example");
        assertThat(response.requiredHeaders()).containsEntry("Content-Type", "video/mp4");
    }

    @Test
    void rejectsUnsupportedMediaType() {
        QuestChallengeUploadRequest request = new QuestChallengeUploadRequest("evidence.pdf", "application/pdf", 1024L);
        when(questChallengeStorageService.maxUploadSizeBytes()).thenReturn(52428800L);

        assertThatThrownBy(() -> service.createUploadInstruction(request, learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("415 UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void rejectsWhenNoActiveQuestChallengeExists() {
        WeeklyQuestWeek inactiveWeek = activeWeek(1L);
        inactiveWeek.setStatus(WeeklyQuestWeekStatus.COMPLETED);
        QuestChallengeUploadRequest request = new QuestChallengeUploadRequest("evidence.mp4", "video/mp4", 1024L);

        when(questChallengeStorageService.maxUploadSizeBytes()).thenReturn(52428800L);
        when(weeklyQuestWeekRepository.findByWeekStartAt(inactiveWeek.getWeekStartAt())).thenReturn(Optional.of(inactiveWeek));

        assertThatThrownBy(() -> service.createUploadInstruction(request, learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
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

    private WeeklyQuestAssignment activeAssignment(WeeklyQuestWeek week) {
        WeeklyQuestAssignment assignment = new WeeklyQuestAssignment();
        ReflectionTestUtils.setField(assignment, "publicId", UUID.randomUUID());
        assignment.setWeek(week);
        assignment.setOfficial(true);
        assignment.setSourceType(WeeklyQuestAssignmentSourceType.ADMIN);
        assignment.setStatus(WeeklyQuestAssignmentStatus.ACTIVE);
        return assignment;
    }
}
