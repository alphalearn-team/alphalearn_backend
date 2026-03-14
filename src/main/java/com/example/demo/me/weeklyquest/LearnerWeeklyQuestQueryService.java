package com.example.demo.me.weeklyquest;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.example.demo.weeklyquest.WeeklyQuestAssignment;
import com.example.demo.weeklyquest.WeeklyQuestAssignmentRepository;
import com.example.demo.weeklyquest.WeeklyQuestCalendarService;
import com.example.demo.weeklyquest.WeeklyQuestChallengeSubmissionRepository;
import com.example.demo.weeklyquest.WeeklyQuestWeek;
import com.example.demo.weeklyquest.WeeklyQuestWeekRepository;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;
import com.example.demo.config.SupabaseAuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearnerWeeklyQuestQueryService {

    private final WeeklyQuestWeekRepository weeklyQuestWeekRepository;
    private final WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;
    private final WeeklyQuestChallengeSubmissionRepository weeklyQuestChallengeSubmissionRepository;
    private final WeeklyQuestCalendarService weeklyQuestCalendarService;

    public LearnerWeeklyQuestQueryService(
            WeeklyQuestWeekRepository weeklyQuestWeekRepository,
            WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository,
            WeeklyQuestChallengeSubmissionRepository weeklyQuestChallengeSubmissionRepository,
            WeeklyQuestCalendarService weeklyQuestCalendarService
    ) {
        this.weeklyQuestWeekRepository = weeklyQuestWeekRepository;
        this.weeklyQuestAssignmentRepository = weeklyQuestAssignmentRepository;
        this.weeklyQuestChallengeSubmissionRepository = weeklyQuestChallengeSubmissionRepository;
        this.weeklyQuestCalendarService = weeklyQuestCalendarService;
    }

    @Transactional(readOnly = true)
    public Optional<LearnerCurrentWeeklyQuestDto> getCurrentWeeklyQuest(SupabaseAuthUser user) {
        OffsetDateTime currentWeekStartAt = weeklyQuestCalendarService.currentWeekStartAt();
        return weeklyQuestWeekRepository.findByWeekStartAt(currentWeekStartAt)
                .filter(week -> week.getStatus() == WeeklyQuestWeekStatus.ACTIVE)
                .flatMap(this::findActiveOfficialAssignment)
                .map(assignment -> LearnerCurrentWeeklyQuestDto.from(
                        assignment,
                        user == null || !user.isLearner() || user.userId() == null
                                ? null
                                : weeklyQuestChallengeSubmissionRepository
                                        .findByLearner_IdAndWeeklyQuestAssignment_Id(user.userId(), assignment.getId())
                                        .map(LearnerQuestChallengeSubmissionSummaryDto::from)
                                        .orElse(null)
                ));
    }

    private Optional<WeeklyQuestAssignment> findActiveOfficialAssignment(WeeklyQuestWeek week) {
        return weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(week.getId())
                .filter(assignment -> assignment.getStatus() == WeeklyQuestAssignmentStatus.ACTIVE);
    }
}
