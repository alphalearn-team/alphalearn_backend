package com.example.demo.learner.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerMapper;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.learner.dto.LearnerProfileDto;
import com.example.demo.learner.dto.LearnerPublicDto;

@ExtendWith(MockitoExtension.class)
class LearnerQueryServiceTest {

    @Mock
    private LearnerRepository learnerRepository;

    @Mock
    private FriendRepository friendRepository;

    private LearnerQueryService service;

    @BeforeEach
    void setUp() {
        service = new LearnerQueryService(learnerRepository, new LearnerMapper(), friendRepository);
    }

    @Test
    void getAllLearnersReturnsRepositoryValues() {
        List<Learner> learners = List.of(new Learner(), new Learner());
        when(learnerRepository.findAll()).thenReturn(learners);

        List<Learner> result = service.getAllLearners();

        assertThat(result).isSameAs(learners);
        verify(learnerRepository).findAll();
    }

    @Test
    void getAllPublicLearnersMapsToPublicDtos() {
        Learner learner = new Learner(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "public-learner",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        learner.setProfilePicture("https://cdn.example.com/public-learner.png");
        when(learnerRepository.findAll()).thenReturn(List.of(learner));

        List<LearnerPublicDto> result = service.getAllPublicLearners();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().publicId()).isEqualTo(learner.getPublicId());
        assertThat(result.getFirst().username()).isEqualTo("public-learner");
        assertThat(result.getFirst().profilePictureUrl()).isEqualTo("https://cdn.example.com/public-learner.png");
    }

    @Test
    void getLearnerProfileReturnsFriendFlagTrueForConfirmedFriends() {
        Learner targetLearner = learner("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "friend-learner");
        targetLearner.setBio("Friend bio");
        targetLearner.setProfilePicture("https://cdn.example.com/friend-learner.png");
        SupabaseAuthUser viewer = learnerUser("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "viewer");

        when(learnerRepository.findByPublicId(targetLearner.getPublicId())).thenReturn(Optional.of(targetLearner));
        when(friendRepository.existsFriendship(viewer.userId(), targetLearner.getId())).thenReturn(true);

        LearnerProfileDto result = service.getLearnerProfile(viewer, targetLearner.getPublicId());

        assertThat(result.publicId()).isEqualTo(targetLearner.getPublicId());
        assertThat(result.username()).isEqualTo("friend-learner");
        assertThat(result.bio()).isEqualTo("Friend bio");
        assertThat(result.profilePictureUrl()).isEqualTo("https://cdn.example.com/friend-learner.png");
        assertThat(result.viewerIsFriend()).isTrue();
    }

    @Test
    void getLearnerProfileReturnsFriendFlagFalseForNonFriends() {
        Learner targetLearner = learner("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "target");
        SupabaseAuthUser viewer = learnerUser("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "viewer");

        when(learnerRepository.findByPublicId(targetLearner.getPublicId())).thenReturn(Optional.of(targetLearner));
        when(friendRepository.existsFriendship(viewer.userId(), targetLearner.getId())).thenReturn(false);

        LearnerProfileDto result = service.getLearnerProfile(viewer, targetLearner.getPublicId());

        assertThat(result.viewerIsFriend()).isFalse();
    }

    @Test
    void getLearnerProfileReturnsFriendFlagFalseForSelf() {
        UUID learnerId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        Learner targetLearner = new Learner(
                learnerId,
                UUID.randomUUID(),
                "viewer",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        SupabaseAuthUser viewer = new SupabaseAuthUser(learnerId, targetLearner, null);

        when(learnerRepository.findByPublicId(targetLearner.getPublicId())).thenReturn(Optional.of(targetLearner));

        LearnerProfileDto result = service.getLearnerProfile(viewer, targetLearner.getPublicId());

        assertThat(result.viewerIsFriend()).isFalse();
    }

    @Test
    void getLearnerProfileReturnsNotFoundWhenLearnerMissing() {
        SupabaseAuthUser viewer = learnerUser("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "viewer");
        UUID targetPublicId = UUID.randomUUID();
        when(learnerRepository.findByPublicId(targetPublicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLearnerProfile(viewer, targetPublicId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException response = (ResponseStatusException) ex;
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void getLearnerProfileRejectsWhenLearnerMissingFromPrincipal() {
        SupabaseAuthUser userWithoutLearner = new SupabaseAuthUser(UUID.randomUUID(), null, null);

        assertThatThrownBy(() -> service.getLearnerProfile(userWithoutLearner, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException response = (ResponseStatusException) ex;
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(response.getReason()).isEqualTo("Learner account required");
                });
    }

    private Learner learner(String id, String username) {
        UUID uuid = UUID.fromString(id);
        return new Learner(
                uuid,
                UUID.randomUUID(),
                username,
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
    }

    private SupabaseAuthUser learnerUser(String id, String username) {
        Learner learner = learner(id, username);
        return new SupabaseAuthUser(learner.getId(), learner, null);
    }
}
