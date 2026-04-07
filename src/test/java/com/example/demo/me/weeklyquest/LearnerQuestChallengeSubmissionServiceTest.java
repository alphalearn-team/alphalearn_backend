package com.example.demo.me.weeklyquest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.storage.r2.QuestChallengeStorageService;
import com.example.demo.weeklyquest.WeeklyQuestAssignment;
import com.example.demo.weeklyquest.WeeklyQuestAssignmentRepository;
import com.example.demo.weeklyquest.WeeklyQuestCalendarService;
import com.example.demo.weeklyquest.WeeklyQuestChallengeSubmission;
import com.example.demo.weeklyquest.WeeklyQuestChallengeSubmissionRepository;
import com.example.demo.weeklyquest.WeeklyQuestChallengeSubmissionTag;
import com.example.demo.weeklyquest.WeeklyQuestWeek;
import com.example.demo.weeklyquest.WeeklyQuestWeekRepository;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
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
class LearnerQuestChallengeSubmissionServiceTest {

    @Mock
    private WeeklyQuestWeekRepository weeklyQuestWeekRepository;
    @Mock
    private WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;
    @Mock
    private WeeklyQuestChallengeSubmissionRepository submissionRepository;
    @Mock
    private QuestChallengeStorageService questChallengeStorageService;
        @Mock
        private LearnerRepository learnerRepository;
        @Mock
        private FriendRepository friendRepository;

    private LearnerQuestChallengeSubmissionService service;
    private SupabaseAuthUser learnerUser;
    private Learner learner;
    private WeeklyQuestWeek activeWeek;
    private WeeklyQuestAssignment activeAssignment;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-22T01:00:00Z"), ZoneId.of("UTC"));
        WeeklyQuestCalendarService calendarService = new WeeklyQuestCalendarService(clock, ZoneId.of("Asia/Singapore"));
        service = new LearnerQuestChallengeSubmissionService(
                weeklyQuestWeekRepository,
                weeklyQuestAssignmentRepository,
                submissionRepository,
                calendarService,
                questChallengeStorageService,
                learnerRepository,
                friendRepository
        );

        UUID learnerId = UUID.randomUUID();
        learner = new Learner(learnerId, UUID.randomUUID(), "learner", OffsetDateTime.parse("2026-03-01T00:00:00Z"), (short) 0);
        learnerUser = new SupabaseAuthUser(learnerId, learner, null);
        activeWeek = activeWeek(1L);
        activeAssignment = activeAssignment(activeWeek);
    }

    @Test
    void createsFirstQuestChallengeSubmission() {
        QuestChallengeSubmissionCommand command = new QuestChallengeSubmissionCommand(
                "quest-challenges/%s/%s/object-evidence.mp4".formatted(activeAssignment.getPublicId(), learner.getId()),
                "evidence.mp4",
                "Learner caption",
                List.of(UUID.randomUUID())
        );
        Learner taggedFriend = new Learner(
                UUID.randomUUID(),
                command.taggedFriendPublicIds().get(0),
                "friend-one",
                OffsetDateTime.parse("2026-03-10T00:00:00Z"),
                (short) 0
        );
        WeeklyQuestChallengeSubmission saved = new WeeklyQuestChallengeSubmission();
        ReflectionTestUtils.setField(saved, "publicId", UUID.randomUUID());
        saved.setLearner(learner);
        saved.setWeeklyQuestAssignment(activeAssignment);
        saved.setMediaObjectKey(command.objectKey());
        saved.setMediaPublicUrl("https://pub.example/" + command.objectKey());
        saved.setMediaContentType("video/mp4");
        saved.setOriginalFilename(command.originalFilename());
        saved.setFileSizeBytes(1024L);
        saved.setCaption("Learner caption");
        saved.setSubmittedAt(OffsetDateTime.parse("2026-03-22T01:00:00+08:00"));
        saved.setUpdatedAt(OffsetDateTime.parse("2026-03-22T01:00:00+08:00"));
        saved.getTaggedFriends().add(new WeeklyQuestChallengeSubmissionTag(
                saved,
                taggedFriend,
                OffsetDateTime.parse("2026-03-22T01:00:00+08:00")
        ));

        mockActiveAssignment();
        when(questChallengeStorageService.expectedObjectKeyPrefix(activeAssignment.getPublicId(), learner.getId()))
                .thenReturn("quest-challenges/%s/%s/".formatted(activeAssignment.getPublicId(), learner.getId()));
        when(questChallengeStorageService.fetchObjectMetadata(command.objectKey()))
                .thenReturn(new QuestChallengeStorageService.StoredObjectMetadata("video/mp4", 1024L, "https://pub.example/" + command.objectKey()));
        when(questChallengeStorageService.maxUploadSizeBytes()).thenReturn(52428800L);
        when(submissionRepository.findByLearner_IdAndWeeklyQuestAssignment_Id(learner.getId(), activeAssignment.getId()))
                .thenReturn(Optional.empty());
        when(learnerRepository.findAllByPublicIdIn(command.taggedFriendPublicIds())).thenReturn(List.of(taggedFriend));
        when(friendRepository.existsFriendship(learner.getId(), taggedFriend.getId())).thenReturn(true);
        when(submissionRepository.save(any(WeeklyQuestChallengeSubmission.class))).thenReturn(saved);

        QuestChallengeSubmissionView result = service.saveCurrentSubmission(command, learnerUser);

        assertThat(result.objectKey()).isEqualTo(command.objectKey());
        assertThat(result.originalFilename()).isEqualTo("evidence.mp4");
        assertThat(result.taggedFriends()).hasSize(1);
        assertThat(result.taggedFriends().get(0).learnerUsername()).isEqualTo("friend-one");
    }

    @Test
    void rejectsObjectKeyOutsideLearnerNamespace() {
        QuestChallengeSubmissionCommand command = new QuestChallengeSubmissionCommand(
                "quest-challenges/other-assignment/other-learner/object.mp4",
                "evidence.mp4",
                null,
                null
        );

        mockActiveAssignment();
        when(questChallengeStorageService.expectedObjectKeyPrefix(activeAssignment.getPublicId(), learner.getId()))
                .thenReturn("quest-challenges/%s/%s/".formatted(activeAssignment.getPublicId(), learner.getId()));

        assertThatThrownBy(() -> service.saveCurrentSubmission(command, learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("objectKey does not belong");
    }

    @Test
    void rejectsMissingUploadedObject() {
        QuestChallengeSubmissionCommand command = new QuestChallengeSubmissionCommand(
                "quest-challenges/%s/%s/object.mp4".formatted(activeAssignment.getPublicId(), learner.getId()),
                "evidence.mp4",
                null,
                null
        );

        mockActiveAssignment();
        when(questChallengeStorageService.expectedObjectKeyPrefix(activeAssignment.getPublicId(), learner.getId()))
                .thenReturn("quest-challenges/%s/%s/".formatted(activeAssignment.getPublicId(), learner.getId()));
        when(questChallengeStorageService.fetchObjectMetadata(command.objectKey()))
                .thenThrow(new IllegalArgumentException("Uploaded object was not found"));

        assertThatThrownBy(() -> service.saveCurrentSubmission(command, learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Uploaded object was not found");
    }

    @Test
    void returnsCurrentSubmissionForLearner() {
        WeeklyQuestChallengeSubmission existing = new WeeklyQuestChallengeSubmission();
        ReflectionTestUtils.setField(existing, "publicId", UUID.randomUUID());
        existing.setLearner(learner);
        existing.setWeeklyQuestAssignment(activeAssignment);
        existing.setMediaObjectKey("quest-challenges/%s/%s/object.mp4".formatted(activeAssignment.getPublicId(), learner.getId()));
        existing.setMediaPublicUrl("https://pub.example/object.mp4");
        existing.setMediaContentType("video/mp4");
        existing.setOriginalFilename("evidence.mp4");
        existing.setFileSizeBytes(1024L);
        existing.setCaption("caption");
        existing.setSubmittedAt(OffsetDateTime.parse("2026-03-22T01:00:00+08:00"));
        existing.setUpdatedAt(OffsetDateTime.parse("2026-03-22T01:05:00+08:00"));

        mockActiveAssignment();
        when(submissionRepository.findByLearner_IdAndWeeklyQuestAssignment_Id(learner.getId(), activeAssignment.getId()))
                .thenReturn(Optional.of(existing));

        Optional<QuestChallengeSubmissionView> result = service.getCurrentSubmission(learnerUser);

        assertThat(result).isPresent();
        assertThat(result.get().originalFilename()).isEqualTo("evidence.mp4");
    }

    @Test
    void rejectsTaggedLearnerWhoIsNotFriend() {
        UUID taggedFriendPublicId = UUID.randomUUID();
        QuestChallengeSubmissionCommand command = new QuestChallengeSubmissionCommand(
                "quest-challenges/%s/%s/object-evidence.mp4".formatted(activeAssignment.getPublicId(), learner.getId()),
                "evidence.mp4",
                "Learner caption",
                List.of(taggedFriendPublicId)
        );
        Learner taggedLearner = new Learner(
                UUID.randomUUID(),
                taggedFriendPublicId,
                "stranger",
                OffsetDateTime.parse("2026-03-10T00:00:00Z"),
                (short) 0
        );

        mockActiveAssignment();
        when(questChallengeStorageService.expectedObjectKeyPrefix(activeAssignment.getPublicId(), learner.getId()))
                .thenReturn("quest-challenges/%s/%s/".formatted(activeAssignment.getPublicId(), learner.getId()));
        when(questChallengeStorageService.fetchObjectMetadata(command.objectKey()))
                .thenReturn(new QuestChallengeStorageService.StoredObjectMetadata("video/mp4", 1024L, "https://pub.example/" + command.objectKey()));
        when(questChallengeStorageService.maxUploadSizeBytes()).thenReturn(52428800L);
        when(learnerRepository.findAllByPublicIdIn(command.taggedFriendPublicIds())).thenReturn(List.of(taggedLearner));
        when(friendRepository.existsFriendship(learner.getId(), taggedLearner.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.saveCurrentSubmission(command, learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tagged learner is not your friend");
    }

    @Test
    void rejectsUnknownTaggedLearner() {
        UUID missingPublicId = UUID.randomUUID();
        QuestChallengeSubmissionCommand command = new QuestChallengeSubmissionCommand(
                "quest-challenges/%s/%s/object-evidence.mp4".formatted(activeAssignment.getPublicId(), learner.getId()),
                "evidence.mp4",
                "Learner caption",
                List.of(missingPublicId)
        );

        mockActiveAssignment();
        when(questChallengeStorageService.expectedObjectKeyPrefix(activeAssignment.getPublicId(), learner.getId()))
                .thenReturn("quest-challenges/%s/%s/".formatted(activeAssignment.getPublicId(), learner.getId()));
        when(questChallengeStorageService.fetchObjectMetadata(command.objectKey()))
                .thenReturn(new QuestChallengeStorageService.StoredObjectMetadata("video/mp4", 1024L, "https://pub.example/" + command.objectKey()));
        when(questChallengeStorageService.maxUploadSizeBytes()).thenReturn(52428800L);
        when(learnerRepository.findAllByPublicIdIn(command.taggedFriendPublicIds())).thenReturn(List.of());

        assertThatThrownBy(() -> service.saveCurrentSubmission(command, learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tagged learner does not exist");
    }

    private void mockActiveAssignment() {
        when(weeklyQuestWeekRepository.findByWeekStartAt(activeWeek.getWeekStartAt())).thenReturn(Optional.of(activeWeek));
        when(weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(activeWeek.getId())).thenReturn(Optional.of(activeAssignment));
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
        ReflectionTestUtils.setField(assignment, "id", 8L);
        ReflectionTestUtils.setField(assignment, "publicId", UUID.randomUUID());
        assignment.setWeek(week);
        assignment.setOfficial(true);
        assignment.setSourceType(WeeklyQuestAssignmentSourceType.ADMIN);
        assignment.setStatus(WeeklyQuestAssignmentStatus.ACTIVE);
        return assignment;
    }
}
