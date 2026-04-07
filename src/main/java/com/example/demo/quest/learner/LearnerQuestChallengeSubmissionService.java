package com.example.demo.quest.learner;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.storage.r2.QuestChallengeStorageService;
import com.example.demo.quest.weekly.WeeklyQuestAssignment;
import com.example.demo.quest.weekly.WeeklyQuestAssignmentRepository;
import com.example.demo.quest.weekly.WeeklyQuestCalendarService;
import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmission;
import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmissionRepository;
import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmissionTag;
import com.example.demo.quest.weekly.WeeklyQuestWeekRepository;
import com.example.demo.quest.weekly.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.quest.weekly.enums.WeeklyQuestWeekStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearnerQuestChallengeSubmissionService {

    private final WeeklyQuestWeekRepository weeklyQuestWeekRepository;
    private final WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;
    private final WeeklyQuestChallengeSubmissionRepository submissionRepository;
    private final WeeklyQuestCalendarService weeklyQuestCalendarService;
    private final QuestChallengeStorageService questChallengeStorageService;
    private final LearnerRepository learnerRepository;
    private final FriendRepository friendRepository;

    public LearnerQuestChallengeSubmissionService(
            WeeklyQuestWeekRepository weeklyQuestWeekRepository,
            WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository,
            WeeklyQuestChallengeSubmissionRepository submissionRepository,
            WeeklyQuestCalendarService weeklyQuestCalendarService,
            QuestChallengeStorageService questChallengeStorageService,
            LearnerRepository learnerRepository,
            FriendRepository friendRepository
    ) {
        this.weeklyQuestWeekRepository = weeklyQuestWeekRepository;
        this.weeklyQuestAssignmentRepository = weeklyQuestAssignmentRepository;
        this.submissionRepository = submissionRepository;
        this.weeklyQuestCalendarService = weeklyQuestCalendarService;
        this.questChallengeStorageService = questChallengeStorageService;
        this.learnerRepository = learnerRepository;
        this.friendRepository = friendRepository;
    }

    @Transactional
    public QuestChallengeSubmissionView saveCurrentSubmission(
            QuestChallengeSubmissionCommand command,
            SupabaseAuthUser user
    ) {
        Learner learner = requireLearner(user);
        if (command == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (command.objectKey() == null || command.objectKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectKey is required");
        }
        if (command.originalFilename() == null || command.originalFilename().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "originalFilename is required");
        }

        WeeklyQuestAssignment assignment = currentActiveAssignment();
        String expectedPrefix = questChallengeStorageService.expectedObjectKeyPrefix(assignment.getPublicId(), learner.getId());
        if (!command.objectKey().startsWith(expectedPrefix)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectKey does not belong to the current learner quest challenge upload");
        }

        QuestChallengeStorageService.StoredObjectMetadata metadata;
        try {
            metadata = questChallengeStorageService.fetchObjectMetadata(command.objectKey());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        if (!isSupportedMediaType(metadata.contentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only image and video uploads are supported");
        }
        if (metadata.fileSizeBytes() > questChallengeStorageService.maxUploadSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded object exceeds the maximum upload size");
        }

        OffsetDateTime now = weeklyQuestCalendarService.now();
        List<Learner> taggedFriends = resolveTaggedFriends(learner, command.taggedFriendPublicIds());
        WeeklyQuestChallengeSubmission submission = submissionRepository
                .findByLearner_IdAndWeeklyQuestAssignment_Id(learner.getId(), assignment.getId())
                .orElseGet(WeeklyQuestChallengeSubmission::new);

        submission.setLearner(learner);
        submission.setWeeklyQuestAssignment(assignment);
        submission.setMediaObjectKey(command.objectKey());
        submission.setMediaPublicUrl(metadata.publicUrl());
        submission.setMediaContentType(metadata.contentType());
        submission.setOriginalFilename(command.originalFilename());
        submission.setFileSizeBytes(metadata.fileSizeBytes());
        submission.setCaption(normalizeCaption(command.caption()));
        submission.setUpdatedAt(now);
        replaceTaggedFriends(submission, taggedFriends, now);
        if (submission.getSubmittedAt() == null) {
            submission.setSubmittedAt(now);
        }

        return QuestChallengeSubmissionView.from(submissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public Optional<QuestChallengeSubmissionView> getCurrentSubmission(SupabaseAuthUser user) {
        Learner learner = requireLearner(user);
        return currentActiveAssignmentOptional()
                .flatMap(assignment -> submissionRepository.findByLearner_IdAndWeeklyQuestAssignment_Id(learner.getId(), assignment.getId()))
                .map(QuestChallengeSubmissionView::from);
    }

    private Optional<WeeklyQuestAssignment> currentActiveAssignmentOptional() {
        OffsetDateTime currentWeekStartAt = weeklyQuestCalendarService.currentWeekStartAt();
        return weeklyQuestWeekRepository.findByWeekStartAt(currentWeekStartAt)
                .filter(candidate -> candidate.getStatus() == WeeklyQuestWeekStatus.ACTIVE)
                .flatMap(week -> weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(week.getId()))
                .filter(assignment -> assignment.getStatus() == WeeklyQuestAssignmentStatus.ACTIVE);
    }

    private WeeklyQuestAssignment currentActiveAssignment() {
        return currentActiveAssignmentOptional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No active quest challenge is available"));
    }

    private Learner requireLearner(SupabaseAuthUser user) {
        if (user == null || !user.isLearner() || user.learner() == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return user.learner();
    }

    private boolean isSupportedMediaType(String contentType) {
        return contentType != null
                && (contentType.startsWith("image/") || contentType.startsWith("video/"));
    }

    private String normalizeCaption(String caption) {
        if (caption == null) {
            return null;
        }
        String normalized = caption.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void replaceTaggedFriends(
            WeeklyQuestChallengeSubmission submission,
            List<Learner> taggedFriends,
            OffsetDateTime now
    ) {
        submission.getTaggedFriends().clear();
        for (Learner taggedFriend : taggedFriends) {
            submission.getTaggedFriends().add(new WeeklyQuestChallengeSubmissionTag(submission, taggedFriend, now));
        }
    }

    private List<Learner> resolveTaggedFriends(Learner learner, List<UUID> taggedFriendPublicIds) {
        List<UUID> normalizedPublicIds = normalizeTaggedFriendPublicIds(taggedFriendPublicIds);
        if (normalizedPublicIds.isEmpty()) {
            return List.of();
        }

        List<Learner> taggedLearners = learnerRepository.findAllByPublicIdIn(normalizedPublicIds);
        Map<UUID, Learner> learnerByPublicId = taggedLearners.stream()
                .collect(Collectors.toMap(Learner::getPublicId, Function.identity()));

        List<Learner> orderedTaggedLearners = new ArrayList<>(normalizedPublicIds.size());
        for (UUID taggedFriendPublicId : normalizedPublicIds) {
            Learner taggedLearner = learnerByPublicId.get(taggedFriendPublicId);
            if (taggedLearner == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tagged learner does not exist: " + taggedFriendPublicId);
            }
            if (learner.getId().equals(taggedLearner.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot tag yourself in a quest submission");
            }
            if (!friendRepository.existsFriendship(learner.getId(), taggedLearner.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tagged learner is not your friend: " + taggedFriendPublicId);
            }
            orderedTaggedLearners.add(taggedLearner);
        }

        return orderedTaggedLearners;
    }

    private List<UUID> normalizeTaggedFriendPublicIds(List<UUID> taggedFriendPublicIds) {
        if (taggedFriendPublicIds == null || taggedFriendPublicIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(taggedFriendPublicIds));
    }
}
