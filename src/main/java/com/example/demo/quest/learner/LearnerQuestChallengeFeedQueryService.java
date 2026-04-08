package com.example.demo.quest.learner;

import java.util.List;
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
        return getFriendsFeed(user, page, size, null);
    }

    @Transactional(readOnly = true)
    public FriendQuestChallengeFeedDto getFriendsFeed(
            SupabaseAuthUser user,
            Integer page,
            Integer size,
            List<UUID> conceptPublicIds
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

        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize);
        boolean hasConcepts = conceptPublicIds != null && !conceptPublicIds.isEmpty();
        Slice<FriendQuestChallengeFeedProjection> slice = hasConcepts
                ? weeklyQuestChallengeSubmissionRepository.findFriendChallengeFeedByLearnerIdAndConceptPublicIds(
                        user.userId(),
                        conceptPublicIds,
                        pageable
                )
                : weeklyQuestChallengeSubmissionRepository.findFriendChallengeFeedByLearnerId(user.userId(), pageable);

        List<UUID> submissionPublicIds = slice.getContent().stream()
                .map(FriendQuestChallengeFeedProjection::getSubmissionPublicId)
                .toList();

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

    @Transactional(readOnly = true)
    public FriendQuestChallengeFeedDto getTaggedHistory(
            SupabaseAuthUser user,
            Integer page,
            Integer size,
            List<UUID> conceptPublicIds
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

        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize);
        boolean hasConcepts = conceptPublicIds != null && !conceptPublicIds.isEmpty();

        Slice<FriendQuestChallengeFeedProjection> slice = hasConcepts
                ? weeklyQuestChallengeSubmissionRepository.findTaggedChallengeFeedByTaggedLearnerIdAndConceptPublicIds(
                        user.userId(),
                        conceptPublicIds,
                        pageable
                )
                : weeklyQuestChallengeSubmissionRepository.findTaggedChallengeFeedByTaggedLearnerId(
                        user.userId(),
                        pageable
                );

        List<UUID> submissionPublicIds = slice.getContent().stream()
                .map(FriendQuestChallengeFeedProjection::getSubmissionPublicId)
                .toList();

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
