package com.example.demo.weeklyquest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.demo.concept.Concept;
import com.example.demo.game.imposter.ImposterWeeklyFeaturedConceptService;
import com.example.demo.weeklyquest.enums.WeeklyQuestActivationSource;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyQuestAutomationService {
    private static final Logger log = LoggerFactory.getLogger(WeeklyQuestAutomationService.class);
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final WeeklyQuestWeekRepository weeklyQuestWeekRepository;
    private final WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;
    private final ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;
    private final WeeklyQuestCalendarService weeklyQuestCalendarService;
    private final Clock clock;

    public WeeklyQuestAutomationService(
            WeeklyQuestWeekRepository weeklyQuestWeekRepository,
            WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository,
            ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService,
            WeeklyQuestCalendarService weeklyQuestCalendarService,
            Clock clock
    ) {
        this.weeklyQuestWeekRepository = weeklyQuestWeekRepository;
        this.weeklyQuestAssignmentRepository = weeklyQuestAssignmentRepository;
        this.imposterWeeklyFeaturedConceptService = imposterWeeklyFeaturedConceptService;
        this.weeklyQuestCalendarService = weeklyQuestCalendarService;
        this.clock = clock;
    }

    @Transactional
    public void activateCurrentWeek() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime currentWeekStartAt = weeklyQuestCalendarService.currentWeekStartAt();
        WeeklyQuestWeek currentWeek = weeklyQuestWeekRepository.findByWeekStartAt(currentWeekStartAt)
                .orElseGet(() -> createScheduledWeek(currentWeekStartAt, now));

        WeeklyQuestAssignment officialAssignment = weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(currentWeek.getId())
                .orElse(null);
        if (officialAssignment == null) {
            officialAssignment = createMonthlyFeaturedAssignment(currentWeek, now);
        }
        if (officialAssignment == null) {
            log.warn(
                    "Weekly quest featured concept is not configured for current month pack. weekStartAt={}, yearMonth={}",
                    currentWeekStartAt,
                    currentYearMonth()
            );
            return;
        }

        if (officialAssignment.getStatus() != WeeklyQuestAssignmentStatus.ACTIVE) {
            officialAssignment.setStatus(WeeklyQuestAssignmentStatus.ACTIVE);
            officialAssignment.setUpdatedAt(now);
            weeklyQuestAssignmentRepository.save(officialAssignment);
        }

        if (currentWeek.getStatus() != WeeklyQuestWeekStatus.ACTIVE) {
            currentWeek.setStatus(WeeklyQuestWeekStatus.ACTIVE);
            currentWeek.setActivatedAt(now);
            currentWeek.setActivationSource(officialAssignment.getSourceType() == WeeklyQuestAssignmentSourceType.FALLBACK
                    ? WeeklyQuestActivationSource.FALLBACK
                    : WeeklyQuestActivationSource.ADMIN);
            weeklyQuestWeekRepository.save(currentWeek);
        }

        retireOlderActiveAssignments(currentWeek.getId(), now);
        completeOlderActiveWeeks(currentWeek.getId());
    }

    private WeeklyQuestWeek createScheduledWeek(OffsetDateTime weekStartAt, OffsetDateTime now) {
        WeeklyQuestWeek week = new WeeklyQuestWeek();
        week.setWeekStartAt(weekStartAt);
        week.setSetupDeadlineAt(weeklyQuestCalendarService.setupDeadlineAt(weekStartAt));
        week.setStatus(WeeklyQuestWeekStatus.SCHEDULED);
        week.setActivationSource(WeeklyQuestActivationSource.ADMIN);
        week.setCreatedAt(now);
        return weeklyQuestWeekRepository.save(week);
    }

    private WeeklyQuestAssignment createMonthlyFeaturedAssignment(WeeklyQuestWeek week, OffsetDateTime now) {
        String yearMonth = currentYearMonth();
        Concept concept = imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept(yearMonth)
                .orElse(null);
        if (concept == null) {
            return null;
        }

        WeeklyQuestAssignment assignment = new WeeklyQuestAssignment();
        assignment.setWeek(week);
        assignment.setConcept(concept);
        assignment.setSlotIndex((short) 0);
        assignment.setOfficial(true);
        assignment.setSourceType(WeeklyQuestAssignmentSourceType.FALLBACK);
        assignment.setStatus(WeeklyQuestAssignmentStatus.ACTIVE);
        assignment.setCreatedAt(now);
        assignment.setUpdatedAt(now);
        return weeklyQuestAssignmentRepository.save(assignment);
    }

    private void retireOlderActiveAssignments(Long currentWeekId, OffsetDateTime now) {
        List<WeeklyQuestWeek> allWeeks = weeklyQuestWeekRepository.findAllByOrderByWeekStartAtAsc();
        for (WeeklyQuestWeek week : allWeeks) {
            if (week.getId().equals(currentWeekId)) {
                continue;
            }
            weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(week.getId())
                    .filter(assignment -> assignment.getStatus() == WeeklyQuestAssignmentStatus.ACTIVE)
                    .ifPresent(assignment -> {
                        assignment.setStatus(WeeklyQuestAssignmentStatus.RETIRED);
                        assignment.setUpdatedAt(now);
                        weeklyQuestAssignmentRepository.save(assignment);
                    });
        }
    }

    private void completeOlderActiveWeeks(Long currentWeekId) {
        List<WeeklyQuestWeek> allWeeks = weeklyQuestWeekRepository.findAllByOrderByWeekStartAtAsc();
        for (WeeklyQuestWeek week : allWeeks) {
            if (week.getId().equals(currentWeekId)) {
                continue;
            }
            if (week.getStatus() == WeeklyQuestWeekStatus.ACTIVE) {
                week.setStatus(WeeklyQuestWeekStatus.COMPLETED);
                weeklyQuestWeekRepository.save(week);
            }
        }
    }

    private String currentYearMonth() {
        LocalDate nowDate = weeklyQuestCalendarService.localDate(weeklyQuestCalendarService.now());
        return YearMonth.from(nowDate).format(YEAR_MONTH_FORMATTER);
    }
}
