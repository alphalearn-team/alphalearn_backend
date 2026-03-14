package com.example.demo.me.weeklyquest;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.example.demo.weeklyquest.WeeklyQuestAssignment;
import com.example.demo.weeklyquest.WeeklyQuestAssignmentRepository;
import com.example.demo.weeklyquest.WeeklyQuestCalendarService;
import com.example.demo.weeklyquest.WeeklyQuestWeek;
import com.example.demo.weeklyquest.WeeklyQuestWeekRepository;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearnerWeeklyQuestQueryService {

    private final WeeklyQuestWeekRepository weeklyQuestWeekRepository;
    private final WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;
    private final WeeklyQuestCalendarService weeklyQuestCalendarService;

    public LearnerWeeklyQuestQueryService(
            WeeklyQuestWeekRepository weeklyQuestWeekRepository,
            WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository,
            WeeklyQuestCalendarService weeklyQuestCalendarService
    ) {
        this.weeklyQuestWeekRepository = weeklyQuestWeekRepository;
        this.weeklyQuestAssignmentRepository = weeklyQuestAssignmentRepository;
        this.weeklyQuestCalendarService = weeklyQuestCalendarService;
    }

    @Transactional(readOnly = true)
    public Optional<LearnerCurrentWeeklyQuestDto> getCurrentWeeklyQuest() {
        OffsetDateTime currentWeekStartAt = weeklyQuestCalendarService.currentWeekStartAt();
        return weeklyQuestWeekRepository.findByWeekStartAt(currentWeekStartAt)
                .filter(week -> week.getStatus() == WeeklyQuestWeekStatus.ACTIVE)
                .flatMap(this::findActiveOfficialAssignment)
                .map(LearnerCurrentWeeklyQuestDto::from);
    }

    private Optional<WeeklyQuestAssignment> findActiveOfficialAssignment(WeeklyQuestWeek week) {
        return weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(week.getId())
                .filter(assignment -> assignment.getStatus() == WeeklyQuestAssignmentStatus.ACTIVE);
    }
}
