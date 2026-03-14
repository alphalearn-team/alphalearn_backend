package com.example.demo.me.weeklyquest;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.learner.Learner;
import com.example.demo.storage.r2.QuestChallengeStorageService;
import com.example.demo.weeklyquest.WeeklyQuestAssignment;
import com.example.demo.weeklyquest.WeeklyQuestAssignmentRepository;
import com.example.demo.weeklyquest.WeeklyQuestCalendarService;
import com.example.demo.weeklyquest.WeeklyQuestChallengeSubmission;
import com.example.demo.weeklyquest.WeeklyQuestChallengeSubmissionRepository;
import com.example.demo.weeklyquest.WeeklyQuestWeekRepository;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
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

    public LearnerQuestChallengeSubmissionService(
            WeeklyQuestWeekRepository weeklyQuestWeekRepository,
            WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository,
            WeeklyQuestChallengeSubmissionRepository submissionRepository,
            WeeklyQuestCalendarService weeklyQuestCalendarService,
            QuestChallengeStorageService questChallengeStorageService
    ) {
        this.weeklyQuestWeekRepository = weeklyQuestWeekRepository;
        this.weeklyQuestAssignmentRepository = weeklyQuestAssignmentRepository;
        this.submissionRepository = submissionRepository;
        this.weeklyQuestCalendarService = weeklyQuestCalendarService;
        this.questChallengeStorageService = questChallengeStorageService;
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
}
