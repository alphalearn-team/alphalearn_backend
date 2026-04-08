package com.example.demo.quest.learner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.learner.Learner;
import com.example.demo.quest.weekly.FriendQuestChallengeFeedProjection;
import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmissionRepository;

@ExtendWith(MockitoExtension.class)
class LearnerQuestChallengeFeedQueryServiceTest {

    @Mock
    private WeeklyQuestChallengeSubmissionRepository weeklyQuestChallengeSubmissionRepository;
    @Mock
    private QuestChallengeTaggedFriendDtoMapper questChallengeTaggedFriendDtoMapper;

    private LearnerQuestChallengeFeedQueryService service;

    @BeforeEach
    void setUp() {
        service = new LearnerQuestChallengeFeedQueryService(
            weeklyQuestChallengeSubmissionRepository,
            questChallengeTaggedFriendDtoMapper
        );
    }

    @Test
    void returnsPaginatedFriendsFeed() {
        UUID learnerId = UUID.randomUUID();
        SupabaseAuthUser user = learnerUser(learnerId);

        FriendQuestChallengeFeedProjection projection = new FriendQuestChallengeFeedProjection() {
            @Override
            public UUID getSubmissionPublicId() {
                return UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            }

            @Override
            public UUID getLearnerPublicId() {
                return UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
            }

            @Override
            public String getLearnerUsername() {
                return "friend-one";
            }

            @Override
            public UUID getConceptPublicId() {
                return UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
            }

            @Override
            public String getConceptTitle() {
                return "Algebra";
            }

            @Override
            public UUID getAssignmentPublicId() {
                return UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
            }

            @Override
            public String getMediaPublicUrl() {
                return "https://example.com/media.mp4";
            }

            @Override
            public String getMediaContentType() {
                return "video/mp4";
            }

            @Override
            public String getOriginalFilename() {
                return "media.mp4";
            }

            @Override
            public String getCaption() {
                return "Great challenge this week";
            }

            @Override
            public OffsetDateTime getSubmittedAt() {
                return OffsetDateTime.parse("2026-03-24T09:00:00Z");
            }
        };

        Slice<FriendQuestChallengeFeedProjection> slice = new SliceImpl<>(
                List.of(projection),
                PageRequest.of(0, 20),
                false
        );

        when(weeklyQuestChallengeSubmissionRepository.findFriendChallengeFeedByLearnerId(learnerId, PageRequest.of(0, 20)))
                .thenReturn(slice);
        when(weeklyQuestChallengeSubmissionRepository.findByPublicIdIn(
            List.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))))
            .thenReturn(List.of());


        FriendQuestChallengeFeedDto result = service.getFriendsFeed(user, 0, 20);

        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).learnerUsername()).isEqualTo("friend-one");
        assertThat(result.items().get(0).conceptTitle()).isEqualTo("Algebra");
    }

    @Test
    void throwsForbiddenWhenUserIsNotLearner() {
        SupabaseAuthUser user = new SupabaseAuthUser(UUID.randomUUID(), null, null);

        assertThatThrownBy(() -> service.getFriendsFeed(user, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void throwsBadRequestWhenPageIsNegative() {
        SupabaseAuthUser user = learnerUser(UUID.randomUUID());

        assertThatThrownBy(() -> service.getFriendsFeed(user, -1, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void throwsBadRequestWhenSizeIsOutOfRange() {
        SupabaseAuthUser user = learnerUser(UUID.randomUUID());

        assertThatThrownBy(() -> service.getFriendsFeed(user, 0, 0))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        assertThatThrownBy(() -> service.getFriendsFeed(user, 0, 51))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    private SupabaseAuthUser learnerUser(UUID learnerId) {
        Learner learner = new Learner(
                learnerId,
                UUID.randomUUID(),
                "learner",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        return new SupabaseAuthUser(learnerId, learner, null);
    }
}
