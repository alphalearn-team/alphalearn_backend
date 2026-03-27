package com.example.demo.friendship.request;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.friendship.friend.Friend;
import com.example.demo.friendship.friend.FriendId;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.friendship.request.dto.FriendRequestDTO;
import com.example.demo.friendship.shared.OrderedUuidPair;
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
        OrderedUuidPair pair = OrderedUuidPair.of(id1, id2);
        return new FriendId(pair.first(), pair.second());
    }

    private FriendRequest reopenRequest(FriendRequest request) {
        request.setStatus(FriendRequestStatus.PENDING);
        request.setCreatedAt(OffsetDateTime.now());
        request.setRespondedAt(null);
        return friendRequestRepository.save(request);
    }

    private FriendRequestDTO mapToDTO(FriendRequest req, Learner currentUser) {

        UUID otherUserId;

        if (req.getSenderId().equals(currentUser.getId())) {
            otherUserId = req.getReceiverId(); // outgoing
        } else {
            otherUserId = req.getSenderId(); // incoming
        }

        Learner otherUser = learnerRepository.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new FriendRequestDTO(
                req.getFriendRequestId(),
                otherUser.getPublicId(),
                otherUser.getUsername(),
                req.getStatus(),
                req.getCreatedAt()
        );
    }

    public FriendRequestDTO sendRequest(Learner currentUser, UUID receiverPublicId) {
        if (receiverPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "receiverPublicId is required");
        }
        if (currentUser != null && receiverPublicId.equals(currentUser.getPublicId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send request to yourself");
        }

        Learner receiver = learnerRepository.findByPublicId(receiverPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UUID senderId = currentUser.getId();
        UUID receiverId = receiver.getId();

        if (senderId.equals(receiverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send request to yourself");
        }

        // already friends?
        FriendId friendId = normalizeFriendId(senderId, receiverId);
        if (friendRepository.existsById(friendId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already friends");
        }

        Optional<FriendRequest> existingOutgoing = friendRequestRepository
                .findBySenderIdAndReceiverId(senderId, receiverId);

        if (existingOutgoing.isPresent()) {
            FriendRequest request = existingOutgoing.get();
            if (request.getStatus() == FriendRequestStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already sent");
            }
            return mapToDTO(reopenRequest(request), currentUser);
        }

        Optional<FriendRequest> existingIncoming = friendRequestRepository
                .findBySenderIdAndReceiverId(receiverId, senderId);

        if (existingIncoming.isPresent() && existingIncoming.get().getStatus() == FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Incoming request already pending");
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

    public void updateRequestStatus(Learner currentUser, Long requestId, FriendRequestStatus status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        if (status == FriendRequestStatus.APPROVED) {
            acceptRequest(currentUser, requestId);
            return;
        }
        if (status == FriendRequestStatus.REJECTED) {
            rejectRequest(currentUser, requestId);
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only APPROVED or REJECTED are supported");
    }

    @Transactional
    public void acceptRequest(Learner currentUser, Long requestId) {

        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        // Only receiver can accept
        if (!request.getReceiverId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to accept this request");
        }

        // Must be pending
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already handled");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        // Only receiver can reject
        if (!request.getReceiverId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to reject this request");
        }

        // Must be pending
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already handled");
        }

        // Update status
        request.setStatus(FriendRequestStatus.REJECTED);
        request.setRespondedAt(OffsetDateTime.now());

        friendRequestRepository.save(request);
    }

    public void cancelRequest(Learner currentUser, Long requestId) {

        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        // 🔥 Only sender can cancel
        if (!request.getSenderId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to cancel this request");
        }

        // 🔥 Only pending can be cancelled
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending requests can be cancelled");
        }

        friendRequestRepository.delete(request);
    }

}
