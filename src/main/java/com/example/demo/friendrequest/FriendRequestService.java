package com.example.demo.friendrequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.friend.FriendId;
import com.example.demo.friend.FriendRepository;
import com.example.demo.friendrequest.dto.FriendRequestDTO;
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

}
