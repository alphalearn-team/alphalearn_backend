package com.example.demo.friendship.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private LearnerRepository learnerRepository;

    private FriendService service;

    @BeforeEach
    void setUp() {
        service = new FriendService(friendRepository, learnerRepository);
    }

    @Test
    void getFriendsIncludesProfilePictureUrl() {
        Learner currentLearner = learner("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "self");
        Learner friend = learner("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "friend");
        friend.setProfilePicture("https://cdn.example.com/friend.png");
        Learner friendWithoutPicture = learner("cccccccc-cccc-cccc-cccc-cccccccccccc", "friend-two");

        when(friendRepository.findFriends(currentLearner.getId())).thenReturn(List.of(
                Friend.builder()
                        .userId1(currentLearner.getId())
                        .userId2(friend.getId())
                        .createdAt(OffsetDateTime.parse("2026-03-01T00:00:00Z"))
                        .build(),
                Friend.builder()
                        .userId1(currentLearner.getId())
                        .userId2(friendWithoutPicture.getId())
                        .createdAt(OffsetDateTime.parse("2026-03-01T00:00:00Z"))
                        .build()
        ));
        when(learnerRepository.findById(friend.getId())).thenReturn(Optional.of(friend));
        when(learnerRepository.findById(friendWithoutPicture.getId())).thenReturn(Optional.of(friendWithoutPicture));

        var friends = service.getFriends(currentLearner);

        assertThat(friends).hasSize(2);
        assertThat(friends.get(0).profilePictureUrl()).isEqualTo("https://cdn.example.com/friend.png");
        assertThat(friends.get(1).profilePictureUrl()).isNull();
    }

    @Test
    void removeFriendRejectsSelfRemoval() {
        Learner currentLearner = learner("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "self");

        when(learnerRepository.findByPublicId(currentLearner.getPublicId())).thenReturn(Optional.of(currentLearner));

        assertThatThrownBy(() -> service.removeFriend(currentLearner, currentLearner.getPublicId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void removeFriendReturnsNotFoundWhenTargetLearnerMissing() {
        Learner currentLearner = learner("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "self");
        UUID targetPublicId = UUID.randomUUID();

        when(learnerRepository.findByPublicId(targetPublicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeFriend(currentLearner, targetPublicId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void removeFriendReturnsNotFoundWhenFriendshipMissing() {
        Learner currentLearner = learner("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "self");
        Learner friend = learner("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "friend");

        when(learnerRepository.findByPublicId(friend.getPublicId())).thenReturn(Optional.of(friend));
        when(friendRepository.existsById(any(FriendId.class))).thenReturn(false);

        assertThatThrownBy(() -> service.removeFriend(currentLearner, friend.getPublicId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
}
