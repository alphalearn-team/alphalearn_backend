package com.example.demo.admin.dashboard;

import com.example.demo.contributor.ContributorRepository;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.lesson.LessonRepository;
import com.example.demo.lessonenrollment.LessonEnrollmentRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private static final int DEFAULT_TOP_CONCEPTS_LIMIT = 5;
    private static final int NEW_CONTRIBUTORS_DAYS_WINDOW = 30;

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
        long lessonsCreated = lessonRepository.countByDeletedAtIsNull();
        long usersSignedUp = learnerRepository.countBy();
        long lessonsEnrolled = lessonEnrollmentRepository.countBy();

        OffsetDateTime newContributorsCutoff = OffsetDateTime.now(clock).minusDays(NEW_CONTRIBUTORS_DAYS_WINDOW);
        long newContributors = contributorRepository.countByDemotedAtIsNullAndPromotedAtGreaterThanEqual(newContributorsCutoff);

        List<AdminDashboardTopConceptDto> topConcepts = lessonRepository
                .findTopConceptsByLessonCount(PageRequest.of(0, DEFAULT_TOP_CONCEPTS_LIMIT))
                .stream()
                .map(view -> new AdminDashboardTopConceptDto(
                        view.getConceptPublicId(),
                        view.getConceptTitle(),
                        view.getLessonCount()
                ))
                .toList();

        return new AdminDashboardSummaryDto(
                lessonsCreated,
                usersSignedUp,
                lessonsEnrolled,
                newContributors,
                topConcepts
        );
    }
}
