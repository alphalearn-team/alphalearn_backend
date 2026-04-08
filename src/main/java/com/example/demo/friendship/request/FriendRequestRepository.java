package com.example.demo.friendship.request;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;


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

    @Query("""
            select request
            from FriendRequest request
            where request.status = com.example.demo.friendship.request.FriendRequestStatus.PENDING
              and (
                    (request.senderId = :userId1 and request.receiverId = :userId2)
                    or
                    (request.senderId = :userId2 and request.receiverId = :userId1)
              )
            """)
    Optional<FriendRequest> findPendingBetween(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}
