package com.example.demo.friendship.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.friendship.friend.Friend;
import com.example.demo.friendship.friend.FriendId;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.friendship.request.dto.FriendRequestDTO;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private LearnerRepository learnerRepository;

    @Mock
    private FriendRepository friendRepository;

    private FriendRequestService service;

    @BeforeEach
    void setUp() {
        service = new FriendRequestService(friendRequestRepository, learnerRepository, friendRepository);
    }

    @Test
    void sendRequestRejectsWhenReversePendingExists() {
        Learner sender = learner("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "sender");
        Learner receiver = learner("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "receiver");

        when(learnerRepository.findByPublicId(receiver.getPublicId())).thenReturn(Optional.of(receiver));
        when(friendRepository.existsById(any(FriendId.class))).thenReturn(false);
        when(friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                sender.getId(),
                receiver.getId(),
                FriendRequestStatus.PENDING
        )).thenReturn(Optional.empty());
        when(friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                receiver.getId(),
                sender.getId(),
                FriendRequestStatus.PENDING
        )).thenReturn(Optional.of(FriendRequest.builder().build()));

        assertThatThrownBy(() -> service.sendRequest(sender, receiver.getPublicId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Incoming request already pending");
    }

    @Test
    void sendRequestAllowsResendAfterRejected() {
        Learner sender = learner("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "sender");
        Learner receiver = learner("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "receiver");

        when(learnerRepository.findByPublicId(receiver.getPublicId())).thenReturn(Optional.of(receiver));
        when(friendRepository.existsById(any(FriendId.class))).thenReturn(false);
        when(friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                sender.getId(),
                receiver.getId(),
                FriendRequestStatus.PENDING
        )).thenReturn(Optional.empty());
        when(friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                receiver.getId(),
                sender.getId(),
                FriendRequestStatus.PENDING
        )).thenReturn(Optional.empty());
        when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(invocation -> {
            FriendRequest value = invocation.getArgument(0, FriendRequest.class);
            value.setFriendRequestId(99L);
            return value;
        });
        when(learnerRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));

        FriendRequestDTO response = service.sendRequest(sender, receiver.getPublicId());

        assertThat(response.requestId()).isEqualTo(99L);
        assertThat(response.otherUserPublicId()).isEqualTo(receiver.getPublicId());
        assertThat(response.status()).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    void acceptRequestApprovesAndCreatesFriendship() {
        Learner receiver = learner("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "receiver");
        UUID senderId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        Long requestId = 42L;
        FriendRequest pending = FriendRequest.builder()
                .friendRequestId(requestId)
                .senderId(senderId)
                .receiverId(receiver.getId())
                .status(FriendRequestStatus.PENDING)
                .createdAt(OffsetDateTime.parse("2026-03-01T00:00:00Z"))
                .build();

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(pending));
        when(friendRepository.existsById(any(FriendId.class))).thenReturn(false);

        service.acceptRequest(receiver, requestId);

        ArgumentCaptor<FriendRequest> requestCaptor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(FriendRequestStatus.APPROVED);
        assertThat(requestCaptor.getValue().getRespondedAt()).isNotNull();

        ArgumentCaptor<Friend> friendCaptor = ArgumentCaptor.forClass(Friend.class);
        verify(friendRepository).save(friendCaptor.capture());
        assertThat(friendCaptor.getValue().getUserId1().toString())
                .isLessThan(friendCaptor.getValue().getUserId2().toString());
    }

    @Test
    void acceptRequestSkipsFriendInsertWhenFriendshipAlreadyExists() {
        Learner receiver = learner("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "receiver");
        UUID senderId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        Long requestId = 11L;
        FriendRequest pending = FriendRequest.builder()
                .friendRequestId(requestId)
                .senderId(senderId)
                .receiverId(receiver.getId())
                .status(FriendRequestStatus.PENDING)
                .createdAt(OffsetDateTime.parse("2026-03-01T00:00:00Z"))
                .build();

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(pending));
        when(friendRepository.existsById(any(FriendId.class))).thenReturn(true);

        service.acceptRequest(receiver, requestId);

        verify(friendRepository, never()).save(any(Friend.class));
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
