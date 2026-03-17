package com.example.demo.friend;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.friend.dto.FriendPublicDTO;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final LearnerRepository learnerRepository;

    public Friend addFriend(UUID userId1, UUID friendPublicId) {

        // get internal IDs
        // Learner user = learnerRepository.findByPublicId(currentUserPublicId)
        //         .orElseThrow(() -> new RuntimeException("User not found"));

        Learner friend = learnerRepository.findByPublicId(friendPublicId)
                .orElseThrow(() -> new RuntimeException("Friend not found"));

        UUID userId2 = friend.getId();

        // prevent self friend
        if (userId1.equals(userId2)) {
            throw new RuntimeException("You cannot add yourself as a friend");
        }

        // normalize order
        if (userId1.compareTo(userId2) > 0) {
            UUID temp = userId1;
            userId1 = userId2;
            userId2 = temp;
        }

        FriendId friendId = new FriendId(userId1, userId2);

        if (friendRepository.existsById(friendId)) {
            throw new RuntimeException("Friendship already exists");
        }

        Friend friendEntity = Friend.builder()
                .userId1(userId1)
                .userId2(userId2)
                .createdAt(OffsetDateTime.now())
                .build();

        return friendRepository.save(friendEntity);
    }


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
                        .orElseThrow(() -> new RuntimeException("Friend not found"));

                return new FriendPublicDTO(
                        friendLearner.getPublicId(),
                        friendLearner.getUsername()
                );

            })
            .toList();
    }

    public void removeFriend(Learner currentLearner, UUID friendPublicId) {

        Learner friend = learnerRepository.findByPublicId(friendPublicId)
                .orElseThrow(() -> new RuntimeException("Friend not found"));

        UUID userId1 = currentLearner.getId();
        UUID userId2 = friend.getId();

        // prevent removing yourself
        if (userId1.equals(userId2)) {
            throw new RuntimeException("You cannot remove yourself");
        }

        // normalize order (IMPORTANT)
        if (userId1.compareTo(userId2) > 0) {
            UUID temp = userId1;
            userId1 = userId2;
            userId2 = temp;
        }

        FriendId friendId = new FriendId(userId1, userId2);

        if (!friendRepository.existsById(friendId)) {
            throw new RuntimeException("Friendship does not exist");
        }

        friendRepository.deleteById(friendId);
    }
}