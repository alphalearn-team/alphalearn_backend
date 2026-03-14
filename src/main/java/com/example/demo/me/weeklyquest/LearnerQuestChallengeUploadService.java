package com.example.demo.me.weeklyquest;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.storage.r2.QuestChallengeStorageService;
import com.example.demo.weeklyquest.WeeklyQuestAssignment;
import com.example.demo.weeklyquest.WeeklyQuestAssignmentRepository;
import com.example.demo.weeklyquest.WeeklyQuestCalendarService;
import com.example.demo.weeklyquest.WeeklyQuestWeek;
import com.example.demo.weeklyquest.WeeklyQuestWeekRepository;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearnerQuestChallengeUploadService {

    private final WeeklyQuestWeekRepository weeklyQuestWeekRepository;
    private final WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;
    private final WeeklyQuestCalendarService weeklyQuestCalendarService;
    private final QuestChallengeStorageService questChallengeStorageService;

    public LearnerQuestChallengeUploadService(
            WeeklyQuestWeekRepository weeklyQuestWeekRepository,
            WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository,
            WeeklyQuestCalendarService weeklyQuestCalendarService,
            QuestChallengeStorageService questChallengeStorageService
    ) {
        this.weeklyQuestWeekRepository = weeklyQuestWeekRepository;
        this.weeklyQuestAssignmentRepository = weeklyQuestAssignmentRepository;
        this.weeklyQuestCalendarService = weeklyQuestCalendarService;
        this.questChallengeStorageService = questChallengeStorageService;
    }

    @Transactional(readOnly = true)
    public QuestChallengeUploadResponse createUploadInstruction(
            QuestChallengeUploadRequest request,
            SupabaseAuthUser user
    ) {
        if (user == null || !user.isLearner() || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.filename() == null || request.filename().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "filename is required");
        }
        if (request.contentType() == null || request.contentType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType is required");
        }
        if (request.fileSizeBytes() == null || request.fileSizeBytes() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileSizeBytes must be greater than 0");
        }
        if (request.fileSizeBytes() > questChallengeStorageService.maxUploadSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "fileSizeBytes exceeds the maximum upload size");
        }
        if (!isSupportedMediaType(request.contentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only image and video uploads are supported");
        }

        WeeklyQuestAssignment assignment = currentActiveAssignment();
        QuestChallengeStorageService.PresignedUpload upload = questChallengeStorageService.generatePresignedUpload(
                assignment.getPublicId(),
                user.userId(),
                request.filename(),
                request.contentType()
        );

        return QuestChallengeUploadResponse.from(assignment.getPublicId(), upload);
    }

    private WeeklyQuestAssignment currentActiveAssignment() {
        OffsetDateTime currentWeekStartAt = weeklyQuestCalendarService.currentWeekStartAt();
        WeeklyQuestWeek week = weeklyQuestWeekRepository.findByWeekStartAt(currentWeekStartAt)
                .filter(candidate -> candidate.getStatus() == WeeklyQuestWeekStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No active quest challenge is available"));

        return weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(week.getId())
                .filter(assignment -> assignment.getStatus() == WeeklyQuestAssignmentStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No active quest challenge is available"));
    }

    private boolean isSupportedMediaType(String contentType) {
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.startsWith("image/") || normalized.startsWith("video/");
    }
}
