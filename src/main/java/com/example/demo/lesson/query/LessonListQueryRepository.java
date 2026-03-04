package com.example.demo.lesson.query;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.demo.concept.Concept;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonModerationStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
public class LessonListQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Lesson> findByCriteria(LessonListCriteria criteria) {
        if (criteria.audience() == LessonListAudience.CONTRIBUTOR && criteria.contributorId() == null) {
            return List.of();
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Lesson> query = cb.createQuery(Lesson.class);
        Root<Lesson> lesson = query.from(Lesson.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isNull(lesson.get("deletedAt")));
        applyAudiencePredicates(criteria, cb, lesson, predicates);

        List<Integer> conceptIds = normalizeConceptIds(criteria.conceptIds());
        if (!conceptIds.isEmpty()) {
            Join<Lesson, Concept> concepts = lesson.join("concepts");
            predicates.add(concepts.get("conceptId").in(conceptIds));
            query.distinct(true);
        }

        query.select(lesson).where(predicates.toArray(Predicate[]::new));
        TypedQuery<Lesson> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    private void applyAudiencePredicates(
            LessonListCriteria criteria,
            CriteriaBuilder cb,
            Root<Lesson> lesson,
            List<Predicate> predicates
    ) {
        switch (criteria.audience()) {
            case PUBLIC -> predicates.add(cb.equal(
                    lesson.get("lessonModerationStatus"),
                    LessonModerationStatus.APPROVED
            ));
            case CONTRIBUTOR -> predicates.add(cb.equal(
                    lesson.get("contributor").get("contributorId"),
                    criteria.contributorId()
            ));
            case ADMIN -> {
                if (criteria.status() != null) {
                    predicates.add(cb.equal(lesson.get("lessonModerationStatus"), criteria.status()));
                }
            }
        }
    }

    private List<Integer> normalizeConceptIds(List<Integer> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Integer> normalized = new LinkedHashSet<>();
        for (Integer conceptId : conceptIds) {
            if (conceptId != null) {
                normalized.add(conceptId);
            }
        }

        return List.copyOf(normalized);
    }
}
