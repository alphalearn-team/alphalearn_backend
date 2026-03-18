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

}