package com.example.demo.admin.weeklyquest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Transactional(readOnly = true)
    public List<WeeklyQuestWeekDto> getWeeks(LocalDate from, LocalDate to) {
        LocalDate resolvedFrom = resolveRangeStart(from, to);
        LocalDate resolvedTo = resolveRangeEnd(resolvedFrom, from, to);
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be on or before to");
        }

        OffsetDateTime fromStart = weeklyQuestCalendarService.weekStartAt(resolvedFrom);
        OffsetDateTime toStart = weeklyQuestCalendarService.weekStartAt(resolvedTo);
        Map<Instant, WeeklyQuestWeek> persistedWeeksByStart = new HashMap<>();
        weeklyQuestWeekRepository.findByWeekStartAtBetweenOrderByWeekStartAtAsc(fromStart, toStart)
                .forEach(week -> persistedWeeksByStart.put(week.getWeekStartAt().toInstant(), week));

        return buildRequestedWeekStarts(resolvedFrom, resolvedTo).stream()
                .map(weeklyQuestCalendarService::weekStartAt)
                .map(weekStartAt -> toWeekDto(persistedWeeksByStart.get(weekStartAt.toInstant()), weekStartAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public WeeklyQuestWeekDto getWeek(String weekStartDate) {
        OffsetDateTime weekStartAt = weeklyQuestCalendarService.parseWeekStartDate(weekStartDate);
        WeeklyQuestWeek week = weeklyQuestWeekRepository.findByWeekStartAt(weekStartAt).orElse(null);
        return toWeekDto(week, weekStartAt);
    }

    @Transactional(readOnly = true)
    public WeeklyQuestWeekDto getCurrentWeek() {
        OffsetDateTime currentWeekStartAt = weeklyQuestCalendarService.currentWeekStartAt();
        WeeklyQuestWeek week = weeklyQuestWeekRepository.findByWeekStartAt(currentWeekStartAt).orElse(null);
        return toWeekDto(week, currentWeekStartAt);
    }

    @Transactional(readOnly = true)
    public List<WeeklyQuestTemplateDto> getTemplates() {
        return questTemplateRepository.findByActiveTrueOrderByTitleAsc().stream()
                .map(WeeklyQuestTemplateDto::from)
                .toList();
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
        return toWeekDto(week, week.getWeekStartAt());
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

    private LocalDate resolveRangeStart(LocalDate from, LocalDate to) {
        if (from != null) {
            weeklyQuestCalendarService.weekStartAt(from);
            return from;
        }
        if (to != null) {
            weeklyQuestCalendarService.weekStartAt(to);
            return to.minusWeeks(8);
        }
        return weeklyQuestCalendarService.localDate(weeklyQuestCalendarService.currentWeekStartAt());
    }

    private LocalDate resolveRangeEnd(LocalDate resolvedFrom, LocalDate from, LocalDate to) {
        if (to != null) {
            weeklyQuestCalendarService.weekStartAt(to);
            return to;
        }
        if (from != null) {
            return from.plusWeeks(8);
        }
        return resolvedFrom.plusWeeks(8);
    }

    private List<LocalDate> buildRequestedWeekStarts(LocalDate from, LocalDate to) {
        long weeksInclusive = ChronoUnit.WEEKS.between(from, to) + 1;
        return java.util.stream.Stream.iterate(from, date -> date.plusWeeks(1))
                .limit(weeksInclusive)
                .toList();
    }

    private WeeklyQuestWeekDto toWeekDto(WeeklyQuestWeek week, OffsetDateTime weekStartAt) {
        long daysUntilDeadline = weeklyQuestCalendarService.daysUntilSetupDeadline(weekStartAt);
        if (week == null) {
            boolean editable = weeklyQuestCalendarService.isEditable(weekStartAt);
            return new WeeklyQuestWeekDto(
                    null,
                    weekStartAt,
                    weeklyQuestCalendarService.setupDeadlineAt(weekStartAt),
                    WeeklyQuestWeekStatus.SCHEDULED,
                    null,
                    null,
                    null,
                    editable,
                    null,
                    true,
                    daysUntilDeadline,
                    editable
            );
        }

        WeeklyQuestAssignmentDto officialAssignment = weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(week.getId())
                .map(WeeklyQuestAssignmentDto::from)
                .orElse(null);
        boolean editable = weeklyQuestCalendarService.isEditable(week.getWeekStartAt()) && week.getStatus() == WeeklyQuestWeekStatus.SCHEDULED;
        boolean unset = officialAssignment == null;
        return WeeklyQuestWeekDto.from(
                week,
                editable,
                officialAssignment,
                unset,
                daysUntilDeadline,
                unset && editable
        );
    }
}
