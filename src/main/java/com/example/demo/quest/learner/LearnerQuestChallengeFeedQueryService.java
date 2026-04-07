package com.example.demo.quest.learner;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.quest.weekly.FriendQuestChallengeFeedProjection;
import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmissionRepository;

@Service
public class LearnerQuestChallengeFeedQueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final WeeklyQuestChallengeSubmissionRepository weeklyQuestChallengeSubmissionRepository;

    public LearnerQuestChallengeFeedQueryService(
            WeeklyQuestChallengeSubmissionRepository weeklyQuestChallengeSubmissionRepository
    ) {
        this.weeklyQuestChallengeSubmissionRepository = weeklyQuestChallengeSubmissionRepository;
    }

    @Transactional(readOnly = true)
    public FriendQuestChallengeFeedDto getFriendsFeed(SupabaseAuthUser user, Integer page, Integer size) {
        if (user == null || !user.isLearner() || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }

        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;

        if (resolvedPage < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be greater than or equal to 0");
        }
        if (resolvedSize <= 0 || resolvedSize > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 50");
        }

        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize);
        Slice<FriendQuestChallengeFeedProjection> slice = weeklyQuestChallengeSubmissionRepository
                .findFriendChallengeFeedByLearnerId(user.userId(), pageable);

        List<FriendQuestChallengeFeedItemDto> items = slice.getContent().stream()
                .map(FriendQuestChallengeFeedItemDto::from)
                .toList();

        return new FriendQuestChallengeFeedDto(items, resolvedPage, resolvedSize, slice.hasNext());
    }
}
