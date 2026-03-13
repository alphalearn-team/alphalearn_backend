package com.example.demo.admin.weeklyquest;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.weeklyquest.QuestTemplate;
import com.example.demo.weeklyquest.QuestTemplateRepository;
import com.example.demo.weeklyquest.WeeklyQuestAssignment;
import com.example.demo.weeklyquest.WeeklyQuestAssignmentRepository;
import com.example.demo.weeklyquest.WeeklyQuestCalendarService;
import com.example.demo.weeklyquest.WeeklyQuestWeek;
import com.example.demo.weeklyquest.WeeklyQuestWeekRepository;
import com.example.demo.weeklyquest.enums.WeeklyQuestActivationSource;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminWeeklyQuestService {

    private final WeeklyQuestWeekRepository weeklyQuestWeekRepository;
    private final WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;
    private final QuestTemplateRepository questTemplateRepository;
    private final ConceptRepository conceptRepository;
    private final WeeklyQuestCalendarService weeklyQuestCalendarService;
    private final Clock clock;

    public AdminWeeklyQuestService(
            WeeklyQuestWeekRepository weeklyQuestWeekRepository,
            WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository,
            QuestTemplateRepository questTemplateRepository,
            ConceptRepository conceptRepository,
            WeeklyQuestCalendarService weeklyQuestCalendarService,
            Clock clock
    ) {
        this.weeklyQuestWeekRepository = weeklyQuestWeekRepository;
        this.weeklyQuestAssignmentRepository = weeklyQuestAssignmentRepository;
        this.questTemplateRepository = questTemplateRepository;
        this.conceptRepository = conceptRepository;
        this.weeklyQuestCalendarService = weeklyQuestCalendarService;
        this.clock = clock;
    }

    @Transactional
    public WeeklyQuestWeekDto upsertOfficialQuest(
            String weekStartDate,
            UpsertWeeklyQuestAssignmentRequest request,
            UUID adminUserId
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.conceptPublicId() == null || request.questTemplatePublicId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conceptPublicId and questTemplatePublicId are required");
        }

        OffsetDateTime targetWeekStartAt = weeklyQuestCalendarService.parseWeekStartDate(weekStartDate);
        if (!weeklyQuestCalendarService.isEditable(targetWeekStartAt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Target week is locked after the 7-day cutoff");
        }

        Concept concept = conceptRepository.findByPublicId(request.conceptPublicId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept not found: " + request.conceptPublicId()));
        QuestTemplate template = questTemplateRepository.findByPublicId(request.questTemplatePublicId())
                .filter(QuestTemplate::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest template not found: " + request.questTemplatePublicId()));

        WeeklyQuestWeek week = weeklyQuestWeekRepository.findByWeekStartAt(targetWeekStartAt)
                .orElseGet(() -> createScheduledWeek(targetWeekStartAt));

        if (week.getStatus() != WeeklyQuestWeekStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only scheduled weeks can be updated");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        WeeklyQuestAssignment assignment = weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(week.getId())
                .orElseGet(() -> newAssignment(week, adminUserId, now));

        assignment.setConcept(concept);
        assignment.setQuestTemplate(template);
        assignment.setOfficial(true);
        assignment.setSourceType(WeeklyQuestAssignmentSourceType.ADMIN);
        assignment.setStatus(WeeklyQuestAssignmentStatus.DRAFT);
        assignment.setCreatedByAdminId(adminUserId);
        assignment.setUpdatedAt(now);
        if (assignment.getCreatedAt() == null) {
            assignment.setCreatedAt(now);
        }

        week.setActivationSource(WeeklyQuestActivationSource.ADMIN);
        weeklyQuestAssignmentRepository.save(assignment);
        weeklyQuestWeekRepository.save(week);
        return toWeekDto(week);
    }

    private WeeklyQuestWeek createScheduledWeek(OffsetDateTime weekStartAt) {
        WeeklyQuestWeek week = new WeeklyQuestWeek();
        week.setWeekStartAt(weekStartAt);
        week.setSetupDeadlineAt(weeklyQuestCalendarService.setupDeadlineAt(weekStartAt));
        week.setStatus(WeeklyQuestWeekStatus.SCHEDULED);
        week.setActivationSource(WeeklyQuestActivationSource.ADMIN);
        week.setCreatedAt(OffsetDateTime.now(clock));
        return weeklyQuestWeekRepository.save(week);
    }

    private WeeklyQuestAssignment newAssignment(WeeklyQuestWeek week, UUID adminUserId, OffsetDateTime now) {
        WeeklyQuestAssignment assignment = new WeeklyQuestAssignment();
        assignment.setWeek(week);
        assignment.setSlotIndex((short) 0);
        assignment.setOfficial(true);
        assignment.setCreatedByAdminId(adminUserId);
        assignment.setCreatedAt(now);
        assignment.setUpdatedAt(now);
        return assignment;
    }

    private WeeklyQuestWeekDto toWeekDto(WeeklyQuestWeek week) {
        WeeklyQuestAssignmentDto officialAssignment = weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(week.getId())
                .map(WeeklyQuestAssignmentDto::from)
                .orElse(null);
        return WeeklyQuestWeekDto.from(
                week,
                weeklyQuestCalendarService.isEditable(week.getWeekStartAt()) && week.getStatus() == WeeklyQuestWeekStatus.SCHEDULED,
                officialAssignment
        );
    }
}
