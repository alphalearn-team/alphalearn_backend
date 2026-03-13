package com.example.demo.weeklyquest;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.weeklyquest.enums.WeeklyQuestActivationSource;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WeeklyQuestAutomationService {
    private static final Logger log = LoggerFactory.getLogger(WeeklyQuestAutomationService.class);
    private static final UUID UNCONFIGURED_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String REMINDER_TYPE_MISSING_OFFICIAL = "MISSING_OFFICIAL_QUEST";

    private final WeeklyQuestWeekRepository weeklyQuestWeekRepository;
    private final WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;
    private final ConceptRepository conceptRepository;
    private final QuestTemplateRepository questTemplateRepository;
    private final WeeklyQuestCalendarService weeklyQuestCalendarService;
    private final WeeklyQuestProperties weeklyQuestProperties;
    private final Clock clock;

    public WeeklyQuestAutomationService(
            WeeklyQuestWeekRepository weeklyQuestWeekRepository,
            WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository,
            ConceptRepository conceptRepository,
            QuestTemplateRepository questTemplateRepository,
            WeeklyQuestCalendarService weeklyQuestCalendarService,
            WeeklyQuestProperties weeklyQuestProperties,
            Clock clock
    ) {
        this.weeklyQuestWeekRepository = weeklyQuestWeekRepository;
        this.weeklyQuestAssignmentRepository = weeklyQuestAssignmentRepository;
        this.conceptRepository = conceptRepository;
        this.questTemplateRepository = questTemplateRepository;
        this.weeklyQuestCalendarService = weeklyQuestCalendarService;
        this.weeklyQuestProperties = weeklyQuestProperties;
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
            officialAssignment = createFallbackAssignment(currentWeek, now);
        }
        if (officialAssignment == null) {
            log.error(
                    "Weekly quest fallback is not configured. Set weekly-quest.fallback.concept-public-id and weekly-quest.fallback.quest-template-public-id before activation. weekStartAt={}",
                    currentWeekStartAt
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

    @Transactional(readOnly = true)
    public void sendMissingUpcomingWeekReminder() {
        OffsetDateTime targetWeekStartAt = weeklyQuestCalendarService.nextSchedulableWeekStartAt();
        OffsetDateTime deadlineAt = weeklyQuestCalendarService.setupDeadlineAt(targetWeekStartAt);
        if (!weeklyQuestCalendarService.now().isBefore(deadlineAt)) {
            return;
        }

        WeeklyQuestWeek week = weeklyQuestWeekRepository.findByWeekStartAt(targetWeekStartAt).orElse(null);
        boolean hasOfficialAssignment = week != null
                && weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(week.getId()).isPresent();
        if (hasOfficialAssignment) {
            return;
        }

        long daysUntilDeadline = weeklyQuestCalendarService.daysUntilSetupDeadline(targetWeekStartAt);
        log.warn(
                "Weekly quest reminder [{}]: week starting {} is still unset. {} days left before setup deadline on {}.",
                REMINDER_TYPE_MISSING_OFFICIAL,
                targetWeekStartAt,
                daysUntilDeadline,
                deadlineAt
        );
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

    private WeeklyQuestAssignment createFallbackAssignment(WeeklyQuestWeek week, OffsetDateTime now) {
        UUID fallbackConceptPublicId = weeklyQuestProperties.fallback().conceptPublicId();
        UUID fallbackTemplatePublicId = weeklyQuestProperties.fallback().questTemplatePublicId();
        if (isUnconfigured(fallbackConceptPublicId) || isUnconfigured(fallbackTemplatePublicId)) {
            return null;
        }

        Concept concept = conceptRepository.findByPublicId(fallbackConceptPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fallback concept not found: " + fallbackConceptPublicId));
        QuestTemplate template = questTemplateRepository.findByPublicId(fallbackTemplatePublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fallback quest template not found: " + fallbackTemplatePublicId));

        WeeklyQuestAssignment assignment = new WeeklyQuestAssignment();
        assignment.setWeek(week);
        assignment.setConcept(concept);
        assignment.setQuestTemplate(template);
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

    private boolean isUnconfigured(UUID value) {
        return value == null || UNCONFIGURED_UUID.equals(value);
    }
}
