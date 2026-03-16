package com.example.demo.admin.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.demo.admin.dashboard.dto.AdminDashboardSummaryDto;
import com.example.demo.contributor.ContributorRepository;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.lesson.LessonRepository;
import com.example.demo.lessonenrollment.LessonEnrollmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LearnerRepository learnerRepository;

    @Mock
    private LessonEnrollmentRepository lessonEnrollmentRepository;

    @Mock
    private ContributorRepository contributorRepository;

    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC);
        adminDashboardService = new AdminDashboardService(
                lessonRepository,
                learnerRepository,
                lessonEnrollmentRepository,
                contributorRepository,
                fixedClock
        );
    }

    @Test
    void getSummaryReturnsExpectedCountsAndTopConcepts() {
        LessonRepository.ConceptLessonCountView first = topConcept(UUID.randomUUID(), "Fractions", 14L);
        LessonRepository.ConceptLessonCountView second = topConcept(UUID.randomUUID(), "Algebra", 9L);

        when(lessonRepository.countByDeletedAtIsNull()).thenReturn(30L);
        when(learnerRepository.countBy()).thenReturn(120L);
        when(lessonEnrollmentRepository.countBy()).thenReturn(280L);
        when(contributorRepository.countByDemotedAtIsNullAndPromotedAtGreaterThanEqual(any(OffsetDateTime.class)))
                .thenReturn(5L);
        when(lessonRepository.findTopConceptsByLessonCount(any(Pageable.class)))
                .thenReturn(List.of(first, second));

        AdminDashboardSummaryDto result = adminDashboardService.getSummary();

        assertThat(result.lessonsCreated()).isEqualTo(30L);
        assertThat(result.usersSignedUp()).isEqualTo(120L);
        assertThat(result.lessonsEnrolled()).isEqualTo(280L);
        assertThat(result.newContributors()).isEqualTo(5L);
        assertThat(result.topConcepts()).hasSize(2);
        assertThat(result.topConcepts().get(0).title()).isEqualTo("Fractions");
        assertThat(result.topConcepts().get(0).lessonCount()).isEqualTo(14L);
        assertThat(result.topConcepts().get(1).title()).isEqualTo("Algebra");
    }

    @Test
    void getSummaryReturnsEmptyTopConceptsWhenNoLessonsHaveConcepts() {
        when(lessonRepository.countByDeletedAtIsNull()).thenReturn(0L);
        when(learnerRepository.countBy()).thenReturn(0L);
        when(lessonEnrollmentRepository.countBy()).thenReturn(0L);
        when(contributorRepository.countByDemotedAtIsNullAndPromotedAtGreaterThanEqual(any(OffsetDateTime.class)))
                .thenReturn(0L);
        when(lessonRepository.findTopConceptsByLessonCount(any(Pageable.class))).thenReturn(List.of());

        AdminDashboardSummaryDto result = adminDashboardService.getSummary();

        assertThat(result.lessonsCreated()).isZero();
        assertThat(result.usersSignedUp()).isZero();
        assertThat(result.lessonsEnrolled()).isZero();
        assertThat(result.newContributors()).isZero();
        assertThat(result.topConcepts()).isEmpty();
    }

        @Test
        void getSummaryWithRangeIncludesOptionalAnalyticsSections() {
        LessonRepository.ConceptLessonCountView top = topConcept(UUID.randomUUID(), "Fractions", 14L);
        LessonRepository.ConceptLessonCountView low = topConcept(UUID.randomUUID(), "Geometry", 1L);

        when(lessonRepository.findTopConceptsByLessonCount(any(Pageable.class)))
            .thenReturn(List.of(top));

        when(lessonRepository.countByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(3L);
        when(learnerRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(2L);
        when(lessonEnrollmentRepository.countByFirstCompletedAtGreaterThanEqualAndFirstCompletedAtLessThan(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(4L);
        when(contributorRepository.countByDemotedAtIsNullAndPromotedAtGreaterThanEqualAndPromotedAtLessThan(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(1L);

        when(lessonRepository.countByDeletedAtIsNullAndLessonModerationStatus(com.example.demo.lesson.LessonModerationStatus.PENDING))
            .thenReturn(12L);
        when(lessonRepository.findLowPerformingConceptsByLessonCount(any(Pageable.class)))
            .thenReturn(List.of(low));

        AdminDashboardSummaryDto result = adminDashboardService.getSummary("7d", null, null);

        assertThat(result.lessonsCreated()).isEqualTo(3L);
        assertThat(result.usersSignedUp()).isEqualTo(2L);
        assertThat(result.lessonsEnrolled()).isEqualTo(4L);
        assertThat(result.newContributors()).isEqualTo(1L);
        assertThat(result.appliedRange()).isEqualTo("7d");
        assertThat(result.startDate()).isEqualTo(java.time.LocalDate.parse("2026-03-09"));
        assertThat(result.endDate()).isEqualTo(java.time.LocalDate.parse("2026-03-15"));
        assertThat(result.comparisonStartDate()).isEqualTo(java.time.LocalDate.parse("2026-03-02"));
        assertThat(result.comparisonEndDate()).isEqualTo(java.time.LocalDate.parse("2026-03-08"));
        assertThat(result.deltas()).isNotNull();
        assertThat(result.trends()).isNotEmpty();
        assertThat(result.alerts()).isNotEmpty();
        assertThat(result.pendingModerationCount()).isEqualTo(12L);
        assertThat(result.lowPerformingConcepts()).hasSize(1);
        assertThat(result.lowPerformingConcepts().get(0).title()).isEqualTo("Geometry");
        }

    private LessonRepository.ConceptLessonCountView topConcept(UUID conceptPublicId, String title, long lessonCount) {
        return new LessonRepository.ConceptLessonCountView() {
            @Override
            public UUID getConceptPublicId() {
                return conceptPublicId;
            }

            @Override
            public String getConceptTitle() {
                return title;
            }

            @Override
            public long getLessonCount() {
                return lessonCount;
            }
        };
    }
}
