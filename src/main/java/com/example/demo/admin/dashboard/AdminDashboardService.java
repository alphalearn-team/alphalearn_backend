package com.example.demo.admin.dashboard;

import com.example.demo.admin.dashboard.dto.AdminDashboardAlertDto;
import com.example.demo.admin.dashboard.dto.AdminDashboardDeltaDto;
import com.example.demo.admin.dashboard.dto.AdminDashboardLowPerformingConceptDto;
import com.example.demo.admin.dashboard.dto.AdminDashboardSummaryDto;
import com.example.demo.admin.dashboard.dto.AdminDashboardTopConceptDto;
import com.example.demo.admin.dashboard.dto.AdminDashboardTrendDto;
import com.example.demo.admin.dashboard.enums.AdminDashboardAlertLevel;
import com.example.demo.contributor.ContributorRepository;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.lesson.LessonRepository;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lessonenrollment.LessonEnrollmentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminDashboardService {

    private static final int DEFAULT_TOP_CONCEPTS_LIMIT = 5;
        private static final int DEFAULT_LOW_PERFORMING_LIMIT = 5;
    private static final int NEW_CONTRIBUTORS_DAYS_WINDOW = 30;
        private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LessonRepository lessonRepository;
    private final LearnerRepository learnerRepository;
    private final LessonEnrollmentRepository lessonEnrollmentRepository;
    private final ContributorRepository contributorRepository;
    private final Clock clock;

    public AdminDashboardService(
            LessonRepository lessonRepository,
            LearnerRepository learnerRepository,
            LessonEnrollmentRepository lessonEnrollmentRepository,
            ContributorRepository contributorRepository,
            Clock clock
    ) {
        this.lessonRepository = lessonRepository;
        this.learnerRepository = learnerRepository;
        this.lessonEnrollmentRepository = lessonEnrollmentRepository;
        this.contributorRepository = contributorRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummaryDto getSummary() {
                return getSummary(null, null, null);
        }

        @Transactional(readOnly = true)
        public AdminDashboardSummaryDto getSummary(String range, LocalDate startDate, LocalDate endDate) {
        List<AdminDashboardTopConceptDto> topConcepts = lessonRepository
                .findTopConceptsByLessonCount(PageRequest.of(0, DEFAULT_TOP_CONCEPTS_LIMIT))
                .stream()
                .map(view -> new AdminDashboardTopConceptDto(
                        view.getConceptPublicId(),
                        view.getConceptTitle(),
                        view.getLessonCount()
                ))
                .toList();

                AnalyticsWindow analyticsWindow = resolveAnalyticsWindow(range, startDate, endDate);
                if (analyticsWindow == null) {
                        long lessonsCreated = lessonRepository.countByDeletedAtIsNull();
                        long usersSignedUp = learnerRepository.countBy();
                        long lessonsEnrolled = lessonEnrollmentRepository.countBy();

                        OffsetDateTime newContributorsCutoff = OffsetDateTime.now(clock).minusDays(NEW_CONTRIBUTORS_DAYS_WINDOW);
                        long newContributors = contributorRepository.countByDemotedAtIsNullAndPromotedAtGreaterThanEqual(newContributorsCutoff);

                        return new AdminDashboardSummaryDto(
                                        lessonsCreated,
                                        usersSignedUp,
                                        lessonsEnrolled,
                                        newContributors,
                                        topConcepts
                        );
                }

                MetricCounts currentPeriod = metricCounts(analyticsWindow.startInclusive(), analyticsWindow.endExclusive());

                MetricCounts previousPeriod = metricCounts(
                                analyticsWindow.comparisonStartDate().atStartOfDay().atOffset(ZoneOffset.UTC),
                                analyticsWindow.comparisonEndDate().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
                );

                AdminDashboardDeltaDto deltas = new AdminDashboardDeltaDto(
                                currentPeriod.lessonsCreated() - previousPeriod.lessonsCreated(),
                                currentPeriod.usersSignedUp() - previousPeriod.usersSignedUp(),
                                currentPeriod.lessonsEnrolled() - previousPeriod.lessonsEnrolled(),
                                currentPeriod.newContributors() - previousPeriod.newContributors()
                );

                List<AdminDashboardTrendDto> trends = buildTrends(analyticsWindow);
                long pendingModerationCount = lessonRepository.countByDeletedAtIsNullAndLessonModerationStatus(LessonModerationStatus.PENDING);
                List<AdminDashboardLowPerformingConceptDto> lowPerformingConcepts = lessonRepository
                                .findLowPerformingConceptsByLessonCount(PageRequest.of(0, DEFAULT_LOW_PERFORMING_LIMIT))
                                .stream()
                                .map(view -> new AdminDashboardLowPerformingConceptDto(
                                                view.getConceptPublicId(),
                                                view.getConceptTitle(),
                                                view.getLessonCount()
                                ))
                                .toList();
                List<AdminDashboardAlertDto> alerts = buildAlerts(pendingModerationCount, lowPerformingConcepts, deltas);

        return new AdminDashboardSummaryDto(
                                currentPeriod.lessonsCreated(),
                                currentPeriod.usersSignedUp(),
                                currentPeriod.lessonsEnrolled(),
                                currentPeriod.newContributors(),
                                topConcepts,
                                deltas,
                                trends,
                                alerts,
                                pendingModerationCount,
                                                                lowPerformingConcepts,
                                                                analyticsWindow.appliedRange(),
                                                                analyticsWindow.startDate(),
                                                                analyticsWindow.endDate(),
                                                                analyticsWindow.comparisonStartDate(),
                                                                analyticsWindow.comparisonEndDate()
        );
    }

        private AnalyticsWindow resolveAnalyticsWindow(String range, LocalDate startDate, LocalDate endDate) {
                boolean hasRange = range != null && !range.isBlank();
                boolean hasStart = startDate != null;
                boolean hasEnd = endDate != null;

                if (!hasRange && !hasStart && !hasEnd) {
                        return null;
                }
                if (hasRange && (hasStart || hasEnd)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use either range or startDate/endDate, not both");
                }
                if (!hasRange && hasStart != hasEnd) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both startDate and endDate are required for custom ranges");
                }

                LocalDate resolvedStart;
                LocalDate resolvedEnd;

                if (hasRange) {
                        String normalizedRange = range.trim().toLowerCase();
                        int days = parseRangeDays(normalizedRange);
                        LocalDate today = LocalDate.now(clock);
                        resolvedEnd = today;
                        resolvedStart = today.minusDays(days - 1L);
                        long totalDays = ChronoUnit.DAYS.between(resolvedStart, resolvedEnd) + 1;
                        LocalDate comparisonStartDate = resolvedStart.minusDays(totalDays);
                        LocalDate comparisonEndDate = resolvedStart.minusDays(1);
                        return new AnalyticsWindow(
                                        normalizedRange,
                                        resolvedStart,
                                        resolvedEnd,
                                        comparisonStartDate,
                                        comparisonEndDate,
                                        resolvedStart.atStartOfDay().atOffset(ZoneOffset.UTC),
                                        resolvedEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC),
                                        totalDays
                        );
                } else {
                        resolvedStart = startDate;
                        resolvedEnd = endDate;
                }

                if (resolvedStart.isAfter(resolvedEnd)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be on or before endDate");
                }

                long totalDays = ChronoUnit.DAYS.between(resolvedStart, resolvedEnd) + 1;
                LocalDate comparisonStartDate = resolvedStart.minusDays(totalDays);
                LocalDate comparisonEndDate = resolvedStart.minusDays(1);
                return new AnalyticsWindow(
                                "custom",
                                resolvedStart,
                                resolvedEnd,
                                comparisonStartDate,
                                comparisonEndDate,
                                resolvedStart.atStartOfDay().atOffset(ZoneOffset.UTC),
                                resolvedEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC),
                                totalDays
                );
        }

        private int parseRangeDays(String range) {
                return switch (range) {
                        case "7d" -> 7;
                        case "30d" -> 30;
                        case "90d" -> 90;
                        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "range must be one of: 7d, 30d, 90d");
                };
        }

        private MetricCounts metricCounts(OffsetDateTime startInclusive, OffsetDateTime endExclusive) {
                return new MetricCounts(
                                lessonRepository.countByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(startInclusive, endExclusive),
                                learnerRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startInclusive, endExclusive),
                                lessonEnrollmentRepository.countByFirstCompletedAtGreaterThanEqualAndFirstCompletedAtLessThan(startInclusive, endExclusive),
                                contributorRepository.countByDemotedAtIsNullAndPromotedAtGreaterThanEqualAndPromotedAtLessThan(startInclusive, endExclusive)
                );
        }

        private List<AdminDashboardTrendDto> buildTrends(AnalyticsWindow window) {
                int bucketSizeDays = window.totalDays() <= 31 ? 1 : 7;
                List<AdminDashboardTrendDto> trends = new ArrayList<>();
                LocalDate cursor = window.startDate();

                while (!cursor.isAfter(window.endDate())) {
                        LocalDate bucketEnd = cursor.plusDays(bucketSizeDays - 1L);
                        if (bucketEnd.isAfter(window.endDate())) {
                                bucketEnd = window.endDate();
                        }

                        OffsetDateTime bucketStartAt = cursor.atStartOfDay().atOffset(ZoneOffset.UTC);
                        OffsetDateTime bucketEndExclusive = bucketEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
                        MetricCounts counts = metricCounts(bucketStartAt, bucketEndExclusive);
                        String label = formatTrendLabel(cursor, bucketEnd);
                        trends.add(new AdminDashboardTrendDto(
                                        label,
                                        counts.lessonsCreated(),
                                        counts.usersSignedUp(),
                                        counts.lessonsEnrolled(),
                                        counts.newContributors()
                        ));

                        cursor = bucketEnd.plusDays(1);
                }

                return trends;
        }

        private String formatTrendLabel(LocalDate start, LocalDate end) {
                if (start.equals(end)) {
                        return DATE_LABEL_FORMATTER.format(start);
                }

                return DATE_LABEL_FORMATTER.format(start) + " to " + DATE_LABEL_FORMATTER.format(end);
        }

        private List<AdminDashboardAlertDto> buildAlerts(
                        long pendingModerationCount,
                        List<AdminDashboardLowPerformingConceptDto> lowPerformingConcepts,
                        AdminDashboardDeltaDto deltas
        ) {
                List<AdminDashboardAlertDto> alerts = new ArrayList<>();

                if (pendingModerationCount >= 25) {
                        alerts.add(new AdminDashboardAlertDto(
                                        "PENDING_MODERATION_SPIKE",
                                        AdminDashboardAlertLevel.CRITICAL,
                                        "Pending moderation count is very high and requires immediate review."
                        ));
                } else if (pendingModerationCount >= 10) {
                        alerts.add(new AdminDashboardAlertDto(
                                        "PENDING_MODERATION_WARNING",
                                        AdminDashboardAlertLevel.WARNING,
                                        "Pending moderation count is building up."
                        ));
                }

                if (!lowPerformingConcepts.isEmpty() && lowPerformingConcepts.get(0).lessonCount() <= 1) {
                        alerts.add(new AdminDashboardAlertDto(
                                        "LOW_PERFORMING_CONCEPTS",
                                        AdminDashboardAlertLevel.INFO,
                                        "Some concepts have very low lesson coverage."
                        ));
                }

                if (deltas.usersSignedUp() < 0) {
                        alerts.add(new AdminDashboardAlertDto(
                                        "SIGNUP_DROP",
                                        AdminDashboardAlertLevel.WARNING,
                                        "User sign-ups dropped versus the previous period."
                        ));
                }

                return alerts;
        }

        private record MetricCounts(
                        long lessonsCreated,
                        long usersSignedUp,
                        long lessonsEnrolled,
                        long newContributors
        ) {
        }

        private record AnalyticsWindow(
                        String appliedRange,
                        LocalDate startDate,
                        LocalDate endDate,
                        LocalDate comparisonStartDate,
                        LocalDate comparisonEndDate,
                        OffsetDateTime startInclusive,
                        OffsetDateTime endExclusive,
                        long totalDays
        ) {
        }
}
