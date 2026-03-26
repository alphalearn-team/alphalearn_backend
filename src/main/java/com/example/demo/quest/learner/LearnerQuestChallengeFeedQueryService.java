package com.example.demo.quest.learner;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.quest.weekly.FriendQuestChallengeFeedProjection;
import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmission;
import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmissionRepository;

@Service
public class LearnerQuestChallengeFeedQueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final OffsetDateTime MIN_SUBMITTED_AT = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final OffsetDateTime MAX_SUBMITTED_AT = OffsetDateTime.parse("9999-12-31T23:59:59Z");

    private final WeeklyQuestChallengeSubmissionRepository weeklyQuestChallengeSubmissionRepository;
    private final QuestChallengeTaggedFriendDtoMapper questChallengeTaggedFriendDtoMapper;

    public LearnerQuestChallengeFeedQueryService(
            WeeklyQuestChallengeSubmissionRepository weeklyQuestChallengeSubmissionRepository,
            QuestChallengeTaggedFriendDtoMapper questChallengeTaggedFriendDtoMapper
    ) {
        this.weeklyQuestChallengeSubmissionRepository = weeklyQuestChallengeSubmissionRepository;
        this.questChallengeTaggedFriendDtoMapper = questChallengeTaggedFriendDtoMapper;
    }

    @Transactional(readOnly = true)
    public FriendQuestChallengeFeedDto getFriendsFeed(SupabaseAuthUser user, Integer page, Integer size) {
        return getFriendsFeed(user, page, size, null, null, null);
    }

    @Transactional(readOnly = true)
    public FriendQuestChallengeFeedDto getFriendsFeed(
            SupabaseAuthUser user,
            Integer page,
            Integer size,
            List<UUID> weekPublicIds,
            OffsetDateTime submittedFrom,
            OffsetDateTime submittedTo
    ) {
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
        if (submittedFrom != null && submittedTo != null && submittedFrom.isAfter(submittedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "submittedFrom must be less than or equal to submittedTo");
        }

        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize);
        Slice<FriendQuestChallengeFeedProjection> slice;
        boolean hasWeeks = weekPublicIds != null && !weekPublicIds.isEmpty();
        boolean hasSubmittedRange = submittedFrom != null || submittedTo != null;
        OffsetDateTime resolvedSubmittedFrom = submittedFrom == null ? MIN_SUBMITTED_AT : submittedFrom;
        OffsetDateTime resolvedSubmittedTo = submittedTo == null ? MAX_SUBMITTED_AT : submittedTo;
        if (hasWeeks) {
            slice = hasSubmittedRange
                ? weeklyQuestChallengeSubmissionRepository.findFriendChallengeFeedByLearnerIdAndWeekPublicIdsAndSubmittedAtRange(
                    user.userId(),
                    weekPublicIds,
                    resolvedSubmittedFrom,
                    resolvedSubmittedTo,
                    pageable
                )
                : weeklyQuestChallengeSubmissionRepository.findFriendChallengeFeedByLearnerIdAndWeekPublicIds(
                    user.userId(),
                    weekPublicIds,
                    pageable
                );
        } else {
            slice = hasSubmittedRange
                ? weeklyQuestChallengeSubmissionRepository.findFriendChallengeFeedByLearnerIdAndSubmittedAtRange(
                    user.userId(),
                    resolvedSubmittedFrom,
                    resolvedSubmittedTo,
                    pageable
                )
                : weeklyQuestChallengeSubmissionRepository.findFriendChallengeFeedByLearnerId(user.userId(), pageable);
        }

        // Extract submission public IDs from projections
        List<UUID> submissionPublicIds = slice.getContent().stream()
                .map(FriendQuestChallengeFeedProjection::getSubmissionPublicId)
                .toList();

        // Fetch full submissions with tagged friends (if there are any)
        final Map<UUID, WeeklyQuestChallengeSubmission> submissionsByPublicId;
        if (!submissionPublicIds.isEmpty()) {
            List<WeeklyQuestChallengeSubmission> submissions = weeklyQuestChallengeSubmissionRepository
                    .findByPublicIdIn(submissionPublicIds);
            submissionsByPublicId = submissions.stream()
                    .collect(Collectors.toMap(
                            WeeklyQuestChallengeSubmission::getPublicId,
                            s -> s
                    ));
        } else {
            submissionsByPublicId = Map.of();
        }

        // Extract submission public IDs from projections
        List<UUID> submissionPublicIds = slice.getContent().stream()
                .map(FriendQuestChallengeFeedProjection::getSubmissionPublicId)
                .toList();

        // Fetch full submissions with tagged friends (if there are any)
        final Map<UUID, WeeklyQuestChallengeSubmission> submissionsByPublicId;
        if (!submissionPublicIds.isEmpty()) {
            List<WeeklyQuestChallengeSubmission> submissions = weeklyQuestChallengeSubmissionRepository
                    .findByPublicIdIn(submissionPublicIds);
            submissionsByPublicId = submissions.stream()
                    .collect(Collectors.toMap(
                            WeeklyQuestChallengeSubmission::getPublicId,
                            s -> s
                    ));
        } else {
            submissionsByPublicId = Map.of();
        }

        List<FriendQuestChallengeFeedItemDto> items = slice.getContent().stream()
                .map(projection -> {
                    WeeklyQuestChallengeSubmission submission = submissionsByPublicId.get(projection.getSubmissionPublicId());
                    List<QuestChallengeTaggedFriendDto> taggedFriends = submission != null
                            ? submission.getTaggedFriends().stream()
                                    .map(questChallengeTaggedFriendDtoMapper::toDto)
                                    .toList()
                            : List.of();
                    return FriendQuestChallengeFeedItemDto.from(projection, taggedFriends);
                })
                .toList();

        return new FriendQuestChallengeFeedDto(items, resolvedPage, resolvedSize, slice.hasNext());
    }
}
