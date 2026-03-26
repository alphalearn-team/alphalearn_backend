package com.example.demo.friendship.request;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findBySenderIdAndReceiverId(UUID senderId, UUID receiverId);

    Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(
            UUID senderId,
            UUID receiverId,
            FriendRequestStatus status
    );

    List<FriendRequest> findByReceiverIdAndStatus(UUID receiverId, FriendRequestStatus status);

    List<FriendRequest> findBySenderIdAndStatus(UUID senderId, FriendRequestStatus status);

    List<FriendRequest> findBySenderId(UUID senderId);
}
