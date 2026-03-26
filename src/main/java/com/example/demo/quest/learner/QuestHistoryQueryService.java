package com.example.demo.quest.learner;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.quest.weekly.QuestHistoryProjection;
import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmissionRepository;
import com.example.demo.quest.weekly.enums.SubmissionVisibility;
import java.time.OffsetDateTime;
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
    private static final OffsetDateTime MIN_SUBMITTED_AT = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final OffsetDateTime MAX_SUBMITTED_AT = OffsetDateTime.parse("9999-12-31T23:59:59Z");
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
        return getMyHistory(user, page, size, weekPublicIds, null, null);
    }

    @Transactional(readOnly = true)
    public QuestHistoryDto getMyHistory(
            SupabaseAuthUser user,
            Integer page,
            Integer size,
            List<UUID> weekPublicIds,
            OffsetDateTime submittedFrom,
            OffsetDateTime submittedTo
    ) {
        UUID learnerId = requireLearnerId(user);
        Pageable pageable = resolvePageable(page, size);
        validateSubmittedAtRange(submittedFrom, submittedTo);
        OffsetDateTime resolvedSubmittedFrom = submittedFrom == null ? MIN_SUBMITTED_AT : submittedFrom;
        OffsetDateTime resolvedSubmittedTo = submittedTo == null ? MAX_SUBMITTED_AT : submittedTo;

        Slice<QuestHistoryProjection> slice;
        if (hasWeeks(weekPublicIds)) {
            slice = hasSubmittedAtFilter(submittedFrom, submittedTo)
                    ? submissionRepository.findMyQuestHistoryByWeekPublicIdsAndSubmittedAtRange(
                            learnerId,
                            weekPublicIds,
                    resolvedSubmittedFrom,
                    resolvedSubmittedTo,
                            pageable
                    )
                    : submissionRepository.findMyQuestHistoryByWeekPublicIds(learnerId, weekPublicIds, pageable);
        } else {
            slice = hasSubmittedAtFilter(submittedFrom, submittedTo)
                ? submissionRepository.findMyQuestHistoryBySubmittedAtRange(learnerId, resolvedSubmittedFrom, resolvedSubmittedTo, pageable)
                    : submissionRepository.findMyQuestHistory(learnerId, pageable);
        }

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
        return getFriendHistory(user, friendPublicId, page, size, weekPublicIds, null, null);
    }

    @Transactional(readOnly = true)
    public QuestHistoryDto getFriendHistory(
            SupabaseAuthUser user,
            UUID friendPublicId,
            Integer page,
            Integer size,
            List<UUID> weekPublicIds,
            OffsetDateTime submittedFrom,
            OffsetDateTime submittedTo
    ) {
        UUID learnerId = requireLearnerId(user);
        Learner friend = learnerRepository.findByPublicId(friendPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend learner not found"));

        if (!friendRepository.existsFriendship(learnerId, friend.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Friend relationship required");
        }

        Pageable pageable = resolvePageable(page, size);
        validateSubmittedAtRange(submittedFrom, submittedTo);
        OffsetDateTime resolvedSubmittedFrom = submittedFrom == null ? MIN_SUBMITTED_AT : submittedFrom;
        OffsetDateTime resolvedSubmittedTo = submittedTo == null ? MAX_SUBMITTED_AT : submittedTo;

        Slice<QuestHistoryProjection> slice;
        if (hasWeeks(weekPublicIds)) {
            slice = hasSubmittedAtFilter(submittedFrom, submittedTo)
                    ? submissionRepository.findFriendQuestHistoryByWeekPublicIdsAndSubmittedAtRange(
                            friend.getId(),
                            FRIEND_VISIBLE,
                            weekPublicIds,
                    resolvedSubmittedFrom,
                    resolvedSubmittedTo,
                            pageable
                    )
                    : submissionRepository.findFriendQuestHistoryByWeekPublicIds(friend.getId(), FRIEND_VISIBLE, weekPublicIds, pageable);
        } else {
            slice = hasSubmittedAtFilter(submittedFrom, submittedTo)
                    ? submissionRepository.findFriendQuestHistoryBySubmittedAtRange(
                            friend.getId(),
                            FRIEND_VISIBLE,
                    resolvedSubmittedFrom,
                    resolvedSubmittedTo,
                            pageable
                    )
                    : submissionRepository.findFriendQuestHistory(friend.getId(), FRIEND_VISIBLE, pageable);
        }

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

    private boolean hasSubmittedAtFilter(OffsetDateTime submittedFrom, OffsetDateTime submittedTo) {
        return submittedFrom != null || submittedTo != null;
    }

    private void validateSubmittedAtRange(OffsetDateTime submittedFrom, OffsetDateTime submittedTo) {
        if (submittedFrom != null && submittedTo != null && submittedFrom.isAfter(submittedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "submittedFrom must be less than or equal to submittedTo");
        }
    }

    private QuestHistoryDto toDto(Slice<QuestHistoryProjection> slice, Pageable pageable) {
        List<QuestHistoryItemDto> items = slice.getContent().stream()
                .map(QuestHistoryItemDto::from)
                .toList();
        return new QuestHistoryDto(items, pageable.getPageNumber(), pageable.getPageSize(), slice.hasNext());
    }
}
