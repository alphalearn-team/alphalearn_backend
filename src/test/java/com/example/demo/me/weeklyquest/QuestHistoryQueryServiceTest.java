package com.example.demo.me.weeklyquest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.weeklyquest.QuestHistoryProjection;
import com.example.demo.weeklyquest.WeeklyQuestChallengeSubmissionRepository;
import com.example.demo.weeklyquest.enums.SubmissionVisibility;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class QuestHistoryQueryServiceTest {

    @Mock
    private WeeklyQuestChallengeSubmissionRepository submissionRepository;
    @Mock
    private LearnerRepository learnerRepository;
    @Mock
    private FriendRepository friendRepository;

    private QuestHistoryQueryService service;
    private SupabaseAuthUser learnerUser;

    @BeforeEach
    void setUp() {
        service = new QuestHistoryQueryService(submissionRepository, learnerRepository, friendRepository);

        UUID learnerId = UUID.randomUUID();
        Learner learner = new Learner(
                learnerId,
                UUID.randomUUID(),
                "learner",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        learnerUser = new SupabaseAuthUser(learnerId, learner, null);
    }

    @Test
    void getsMyHistory() {
        when(submissionRepository.findMyQuestHistory(eq(learnerUser.userId()), eq(PageRequest.of(0, 20))))
                .thenReturn(new SliceImpl<>(List.of()));

        QuestHistoryDto result = service.getMyHistory(learnerUser, 0, 20, null);

        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void getsMyHistoryBySubmittedAtRange() {
        OffsetDateTime submittedFrom = OffsetDateTime.parse("2026-03-01T00:00:00Z");
        OffsetDateTime submittedTo = OffsetDateTime.parse("2026-03-31T23:59:59Z");

        when(submissionRepository.findMyQuestHistoryBySubmittedAtRange(
                eq(learnerUser.userId()),
                eq(submittedFrom),
                eq(submittedTo),
                eq(PageRequest.of(0, 20))
        )).thenReturn(new SliceImpl<>(List.of()));

        QuestHistoryDto result = service.getMyHistory(learnerUser, 0, 20, null, submittedFrom, submittedTo);

        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void rejectsMyHistoryWhenSubmittedRangeIsInvalid() {
        OffsetDateTime submittedFrom = OffsetDateTime.parse("2026-04-01T00:00:00Z");
        OffsetDateTime submittedTo = OffsetDateTime.parse("2026-03-01T00:00:00Z");

        assertThatThrownBy(() -> service.getMyHistory(learnerUser, 0, 20, null, submittedFrom, submittedTo))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("submittedFrom must be less than or equal to submittedTo");
    }

    @Test
    void rejectsFriendHistoryWhenNotFriends() {
        UUID friendPublicId = UUID.randomUUID();
        Learner friend = new Learner(
                UUID.randomUUID(),
                friendPublicId,
                "friend",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );

        when(learnerRepository.findByPublicId(friendPublicId)).thenReturn(Optional.of(friend));
        when(friendRepository.existsFriendship(learnerUser.userId(), friend.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.getFriendHistory(learnerUser, friendPublicId, 0, 20, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Friend relationship required");
    }

    @Test
    void getsFriendHistoryWhenFriends() {
        UUID friendPublicId = UUID.randomUUID();
        Learner friend = new Learner(
                UUID.randomUUID(),
                friendPublicId,
                "friend",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );

        when(learnerRepository.findByPublicId(friendPublicId)).thenReturn(Optional.of(friend));
        when(friendRepository.existsFriendship(learnerUser.userId(), friend.getId())).thenReturn(true);
        when(submissionRepository.findFriendQuestHistory(
                eq(friend.getId()),
                eq(List.of(SubmissionVisibility.PUBLIC, SubmissionVisibility.FRIENDS)),
                any()))
                .thenReturn(new SliceImpl<>(List.of()));

        QuestHistoryDto result = service.getFriendHistory(learnerUser, friendPublicId, 0, 20, null);

        assertThat(result.items()).isEmpty();
    }

    @Test
    void getsFriendHistoryBySubmittedAtRangeWhenFriends() {
        UUID friendPublicId = UUID.randomUUID();
        Learner friend = new Learner(
                UUID.randomUUID(),
                friendPublicId,
                "friend",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        OffsetDateTime submittedFrom = OffsetDateTime.parse("2026-03-01T00:00:00Z");
        OffsetDateTime submittedTo = OffsetDateTime.parse("2026-03-31T23:59:59Z");

        when(learnerRepository.findByPublicId(friendPublicId)).thenReturn(Optional.of(friend));
        when(friendRepository.existsFriendship(learnerUser.userId(), friend.getId())).thenReturn(true);
        when(submissionRepository.findFriendQuestHistoryBySubmittedAtRange(
                eq(friend.getId()),
                eq(List.of(SubmissionVisibility.PUBLIC, SubmissionVisibility.FRIENDS)),
                eq(submittedFrom),
                eq(submittedTo),
                any()))
                .thenReturn(new SliceImpl<>(List.of()));

        QuestHistoryDto result = service.getFriendHistory(
                learnerUser,
                friendPublicId,
                0,
                20,
                null,
                submittedFrom,
                submittedTo
        );

        assertThat(result.items()).isEmpty();
    }

    @Test
    void rejectsFriendHistoryWhenSubmittedRangeIsInvalid() {
        UUID friendPublicId = UUID.randomUUID();
        Learner friend = new Learner(
                UUID.randomUUID(),
                friendPublicId,
                "friend",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        OffsetDateTime submittedFrom = OffsetDateTime.parse("2026-04-01T00:00:00Z");
        OffsetDateTime submittedTo = OffsetDateTime.parse("2026-03-01T00:00:00Z");

        when(learnerRepository.findByPublicId(friendPublicId)).thenReturn(Optional.of(friend));
        when(friendRepository.existsFriendship(learnerUser.userId(), friend.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.getFriendHistory(
                learnerUser,
                friendPublicId,
                0,
                20,
                null,
                submittedFrom,
                submittedTo
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("submittedFrom must be less than or equal to submittedTo");
    }

    @Test
    void supportsOneSidedSubmittedRangeForFriendHistory() {
        UUID friendPublicId = UUID.randomUUID();
        Learner friend = new Learner(
                UUID.randomUUID(),
                friendPublicId,
                "friend",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        OffsetDateTime submittedFrom = OffsetDateTime.parse("2026-03-14T00:00:00Z");
        OffsetDateTime normalizedSubmittedTo = OffsetDateTime.parse("9999-12-31T23:59:59Z");

        when(learnerRepository.findByPublicId(friendPublicId)).thenReturn(Optional.of(friend));
        when(friendRepository.existsFriendship(learnerUser.userId(), friend.getId())).thenReturn(true);
        when(submissionRepository.findFriendQuestHistoryBySubmittedAtRange(
                eq(friend.getId()),
                eq(List.of(SubmissionVisibility.PUBLIC, SubmissionVisibility.FRIENDS)),
                eq(submittedFrom),
                eq(normalizedSubmittedTo),
                any()
        )).thenReturn(new SliceImpl<>(List.of()));

        QuestHistoryDto result = service.getFriendHistory(
                learnerUser,
                friendPublicId,
                0,
                20,
                null,
                submittedFrom,
                null
        );

        assertThat(result.items()).isEmpty();
    }

    @Test
    void getsPublicHistory() {
        UUID learnerPublicId = UUID.randomUUID();
        Learner learner = new Learner(
                UUID.randomUUID(),
                learnerPublicId,
                "public-learner",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        when(learnerRepository.findByPublicId(learnerPublicId)).thenReturn(Optional.of(learner));
        when(submissionRepository.findPublicQuestHistory(eq(learner.getId()), eq(SubmissionVisibility.PUBLIC), any()))
                .thenReturn(new SliceImpl<>(List.<QuestHistoryProjection>of()));

        QuestHistoryDto result = service.getPublicHistory(learnerPublicId, 0, 20, null);

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(0);
    }
}
