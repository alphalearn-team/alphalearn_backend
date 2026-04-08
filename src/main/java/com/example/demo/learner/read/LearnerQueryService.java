package com.example.demo.learner.read;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerMapper;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.learner.dto.LearnerProfileDto;
import com.example.demo.learner.dto.LearnerPublicDto;

@Service
public class LearnerQueryService {
    
    private final LearnerRepository learnerRepository;
    private final LearnerMapper learnerMapper;
    private final FriendRepository friendRepository;

    public LearnerQueryService(
            LearnerRepository learnerRepository,
            LearnerMapper learnerMapper,
            FriendRepository friendRepository
    ) {
        this.learnerRepository = learnerRepository;
        this.learnerMapper = learnerMapper;
        this.friendRepository = friendRepository;
    }

    public List<Learner> getAllLearners() {
        return learnerRepository.findAll();
    }

    public List<LearnerPublicDto> getAllPublicLearners() {
        return learnerRepository.findAll().stream()
                .map(learnerMapper::toPublicDto)
                .toList();
    }

    public LearnerProfileDto getLearnerProfile(SupabaseAuthUser user, UUID learnerPublicId) {
        UUID viewerLearnerId = requireLearnerId(user);
        Learner learner = learnerRepository.findByPublicId(learnerPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learner not found"));

        boolean viewerIsFriend = !viewerLearnerId.equals(learner.getId())
                && friendRepository.existsFriendship(viewerLearnerId, learner.getId());

        return new LearnerProfileDto(
                learner.getPublicId(),
                learner.getUsername(),
                learner.getBio(),
                learner.getProfilePicture(),
                viewerIsFriend
        );
    }

    private UUID requireLearnerId(SupabaseAuthUser user) {
        if (user == null || !user.isLearner() || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return user.userId();
    }
}
