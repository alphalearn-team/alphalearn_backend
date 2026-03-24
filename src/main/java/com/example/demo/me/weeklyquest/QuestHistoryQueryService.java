package com.example.demo.me.weeklyquest;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friend.FriendRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.weeklyquest.QuestHistoryProjection;
import com.example.demo.weeklyquest.WeeklyQuestChallengeSubmissionRepository;
import com.example.demo.weeklyquest.enums.SubmissionVisibility;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuestHistoryQueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final List<SubmissionVisibility> FRIEND_VISIBLE = List.of(
            SubmissionVisibility.PUBLIC,
            SubmissionVisibility.FRIENDS
    );

    private final WeeklyQuestChallengeSubmissionRepository submissionRepository;
    private final LearnerRepository learnerRepository;
    private final FriendRepository friendRepository;

    public QuestHistoryQueryService(
            WeeklyQuestChallengeSubmissionRepository submissionRepository,
            LearnerRepository learnerRepository,
            FriendRepository friendRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.learnerRepository = learnerRepository;
        this.friendRepository = friendRepository;
    }

    @Transactional(readOnly = true)
    public QuestHistoryDto getMyHistory(SupabaseAuthUser user, Integer page, Integer size, List<UUID> weekPublicIds) {
        UUID learnerId = requireLearnerId(user);
        Pageable pageable = resolvePageable(page, size);
        Slice<QuestHistoryProjection> slice = hasWeeks(weekPublicIds)
                ? submissionRepository.findMyQuestHistoryByWeekPublicIds(learnerId, weekPublicIds, pageable)
                : submissionRepository.findMyQuestHistory(learnerId, pageable);
        return toDto(slice, pageable);
    }

    @Transactional(readOnly = true)
    public QuestHistoryDto getFriendHistory(
            SupabaseAuthUser user,
            UUID friendPublicId,
            Integer page,
            Integer size,
            List<UUID> weekPublicIds
    ) {
        UUID learnerId = requireLearnerId(user);
        Learner friend = learnerRepository.findByPublicId(friendPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend learner not found"));

        if (!friendRepository.existsFriendship(learnerId, friend.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Friend relationship required");
        }

        Pageable pageable = resolvePageable(page, size);
        Slice<QuestHistoryProjection> slice = hasWeeks(weekPublicIds)
                ? submissionRepository.findFriendQuestHistoryByWeekPublicIds(friend.getId(), FRIEND_VISIBLE, weekPublicIds, pageable)
                : submissionRepository.findFriendQuestHistory(friend.getId(), FRIEND_VISIBLE, pageable);
        return toDto(slice, pageable);
    }

    @Transactional(readOnly = true)
    public QuestHistoryDto getPublicHistory(UUID learnerPublicId, Integer page, Integer size, List<UUID> weekPublicIds) {
        Learner learner = learnerRepository.findByPublicId(learnerPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learner not found"));

        Pageable pageable = resolvePageable(page, size);
        Slice<QuestHistoryProjection> slice = hasWeeks(weekPublicIds)
                ? submissionRepository.findPublicQuestHistoryByWeekPublicIds(
                        learner.getId(),
                        SubmissionVisibility.PUBLIC,
                        weekPublicIds,
                        pageable
                )
                : submissionRepository.findPublicQuestHistory(learner.getId(), SubmissionVisibility.PUBLIC, pageable);
        return toDto(slice, pageable);
    }

    private UUID requireLearnerId(SupabaseAuthUser user) {
        if (user == null || !user.isLearner() || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return user.userId();
    }

    private Pageable resolvePageable(Integer page, Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;

        if (resolvedPage < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be greater than or equal to 0");
        }
        if (resolvedSize <= 0 || resolvedSize > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 50");
        }

        return PageRequest.of(resolvedPage, resolvedSize);
    }

    private boolean hasWeeks(List<UUID> weekPublicIds) {
        return weekPublicIds != null && !weekPublicIds.isEmpty();
    }

    private QuestHistoryDto toDto(Slice<QuestHistoryProjection> slice, Pageable pageable) {
        List<QuestHistoryItemDto> items = slice.getContent().stream()
                .map(QuestHistoryItemDto::from)
                .toList();
        return new QuestHistoryDto(items, pageable.getPageNumber(), pageable.getPageSize(), slice.hasNext());
    }
}
