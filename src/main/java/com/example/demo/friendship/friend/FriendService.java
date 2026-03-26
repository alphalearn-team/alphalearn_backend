package com.example.demo.friendship.friend;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.friendship.friend.dto.FriendPublicDTO;
import com.example.demo.friendship.shared.OrderedUuidPair;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final LearnerRepository learnerRepository;


    public List<FriendPublicDTO> getFriends(Learner currentLearner) {

        List<Friend> friendships = friendRepository.findFriends(currentLearner.getId());

        return friendships.stream()
            .map(f -> {

                UUID friendId;

                if (f.getUserId1().equals(currentLearner.getId())) {
                    friendId = f.getUserId2();
                } else {
                    friendId = f.getUserId1();
                }

                Learner friendLearner = learnerRepository.findById(friendId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found"));

                return new FriendPublicDTO(
                        friendLearner.getPublicId(),
                        friendLearner.getUsername()
                );

            })
            .toList();
    }

    public void removeFriend(Learner currentLearner, UUID friendPublicId) {
        if (friendPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "friendPublicId is required");
        }

        Learner friend = learnerRepository.findByPublicId(friendPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found"));

        UUID userId1 = currentLearner.getId();
        UUID userId2 = friend.getId();

        // prevent removing yourself
        if (userId1.equals(userId2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot remove yourself");
        }

        OrderedUuidPair pair = OrderedUuidPair.of(userId1, userId2);
        FriendId friendId = new FriendId(pair.first(), pair.second());

        if (!friendRepository.existsById(friendId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship does not exist");
        }

        friendRepository.deleteById(friendId);
    }

}
