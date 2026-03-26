package com.example.demo.friendship.request;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.friendship.friend.Friend;
import com.example.demo.friendship.friend.FriendId;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.friendship.request.dto.FriendRequestDTO;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final LearnerRepository learnerRepository;
    private final FriendRepository friendRepository;

    private FriendId normalizeFriendId(UUID id1, UUID id2) {
        if (id1.compareTo(id2) > 0) {
            return new FriendId(id2, id1);
        }
        return new FriendId(id1, id2);
    }

    private FriendRequestDTO mapToDTO(FriendRequest req, Learner currentUser) {

        UUID otherUserId;

        if (req.getSenderId().equals(currentUser.getId())) {
            otherUserId = req.getReceiverId(); // outgoing
        } else {
            otherUserId = req.getSenderId(); // incoming
        }

        Learner otherUser = learnerRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new FriendRequestDTO(
                req.getFriendRequestId(),
                otherUser.getPublicId(),
                otherUser.getUsername(),
                req.getStatus(),
                req.getCreatedAt()
        );
    }

    public FriendRequestDTO sendRequest(Learner currentUser, UUID receiverPublicId) {
        Learner receiver = learnerRepository.findByPublicId(receiverPublicId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID senderId = currentUser.getId();
        UUID receiverId = receiver.getId();

        if (senderId.equals(receiverId)) {
            throw new RuntimeException("Cannot send request to yourself");
        }

        // already friends?
        FriendId friendId = normalizeFriendId(senderId, receiverId);
        if (friendRepository.existsById(friendId)) {
            throw new RuntimeException("Already friends");
        }

        // existing request?
        Optional<FriendRequest> existing =
                friendRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId);

        if (existing.isPresent()) {
            throw new RuntimeException("Request already sent");
        }

        FriendRequest request = FriendRequest.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .status(FriendRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        FriendRequest saved = friendRequestRepository.save(request);
        return mapToDTO(saved, currentUser);
    }

    public List<FriendRequestDTO> getPendingRequests(Learner currentUser) {

        List<FriendRequest> requests =
                friendRequestRepository.findByReceiverIdAndStatus(
                        currentUser.getId(),
                        FriendRequestStatus.PENDING
                );

        return requests.stream()
                .map(req -> mapToDTO(req, currentUser))
                .toList();
    }

    
    public List<FriendRequestDTO> getOutgoingRequests(Learner currentUser) {

        List<FriendRequest> requests =
                friendRequestRepository.findBySenderIdAndStatus(
                        currentUser.getId(),
                        FriendRequestStatus.PENDING
                );

        return requests.stream()
                .map(req -> mapToDTO(req, currentUser))
                .toList();
    }

    public void acceptRequest(Learner currentUser, Long requestId) {

        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Only receiver can accept
        if (!request.getReceiverId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to accept this request");
        }

        // Must be pending
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Request already handled");
        }

        // Update status
        request.setStatus(FriendRequestStatus.APPROVED);
        request.setRespondedAt(OffsetDateTime.now());

        friendRequestRepository.save(request);

        // CREATE FRIEND
        FriendId friendId = normalizeFriendId(
                request.getSenderId(),
                request.getReceiverId()
        );

        // safety check (optional but good)
        if (!friendRepository.existsById(friendId)) {

            Friend friend = Friend.builder()
                    .userId1(friendId.getUserId1())
                    .userId2(friendId.getUserId2())
                    .createdAt(OffsetDateTime.now())
                    .build();

            friendRepository.save(friend);
        }
    }

    public void rejectRequest(Learner currentUser, Long requestId) {

        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Only receiver can reject
        if (!request.getReceiverId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to reject this request");
        }

        // Must be pending
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Request already handled");
        }

        // Update status
        request.setStatus(FriendRequestStatus.REJECTED);
        request.setRespondedAt(OffsetDateTime.now());

        friendRequestRepository.save(request);
    }

    public void cancelRequest(Learner currentUser, Long requestId) {

        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // 🔥 Only sender can cancel
        if (!request.getSenderId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to cancel this request");
        }

        // 🔥 Only pending can be cancelled
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be cancelled");
        }

        friendRequestRepository.delete(request);
    }

}
