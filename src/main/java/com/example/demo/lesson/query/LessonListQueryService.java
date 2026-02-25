package com.example.demo.lesson.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.contributor.ContributorRepository;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonRepository;
import com.example.demo.lesson.enums.LessonModerationStatus;

@Service
public class LessonListQueryService {

    private final LessonRepository lessonRepository;
    private final ContributorRepository contributorRepository;

    public LessonListQueryService(
            LessonRepository lessonRepository,
            ContributorRepository contributorRepository
    ) {
        this.lessonRepository = lessonRepository;
        this.contributorRepository = contributorRepository;
    }

    @Transactional(readOnly = true)
    public List<Lesson> findLessons(LessonListCriteria criteria) {
        return switch (criteria.audience()) {
            case PUBLIC -> findPublicLessons(criteria);
            case CONTRIBUTOR -> findContributorLessons(criteria);
            case ADMIN -> throw new IllegalArgumentException("ADMIN audience not supported yet");
        };
    }

    private List<Lesson> findPublicLessons(LessonListCriteria criteria) {
        if (!hasConceptFilter(criteria.conceptIds())) {
            return lessonRepository.findByLessonModerationStatusAndDeletedAtIsNull(LessonModerationStatus.APPROVED);
        }

        return switch (criteria.conceptsMatch()) {
            case ANY -> lessonRepository.findByConceptIds(criteria.conceptIds());
            case ALL -> lessonRepository.findByAllConceptIds(criteria.conceptIds(), criteria.conceptIds().size());
        };
    }

    private List<Lesson> findContributorLessons(LessonListCriteria criteria) {
        UUID contributorId = criteria.contributorId();
        if (contributorId == null || !contributorRepository.existsById(contributorId)) {
            return List.of();
        }

        if (!hasConceptFilter(criteria.conceptIds())) {
            return lessonRepository.findByContributor_ContributorIdAndDeletedAtIsNull(contributorId);
        }

        return switch (criteria.conceptsMatch()) {
            case ANY -> lessonRepository.findByContributorAndConceptIds(contributorId, criteria.conceptIds());
            case ALL -> lessonRepository.findByContributorAndAllConceptIds(
                    contributorId,
                    criteria.conceptIds(),
                    criteria.conceptIds().size()
            );
        };
    }

    private boolean hasConceptFilter(List<Integer> conceptIds) {
        return conceptIds != null && !conceptIds.isEmpty();
    }
}
