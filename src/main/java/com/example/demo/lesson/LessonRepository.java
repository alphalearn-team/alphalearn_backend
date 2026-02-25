package com.example.demo.lesson;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    List<Lesson> findByDeletedAtIsNull();
    List<Lesson> findByContributor_ContributorIdAndDeletedAtIsNull(UUID contributorId);
    boolean existsByContributor_ContributorId(UUID contributorId);
    List<Lesson> findByLessonModerationStatusAndDeletedAtIsNull(LessonModerationStatus lessonModerationStatus);

    @Query(
            value = """
                select distinct l.*
                from lessons l
                join lesson_concepts lc on lc.lesson_id = l.lesson_id
                where lc.concept_id in (:conceptIds)
                  and l.moderation_status = 'APPROVED'
                  and l.deleted_at is null
            """,
            nativeQuery = true
    )
    List<Lesson> findByConceptIds(@Param("conceptIds") List<Integer> conceptIds);

    @Query(
            value = """
                select distinct l.*
                from lessons l
                join lesson_concepts lc on lc.lesson_id = l.lesson_id
                where lc.concept_id in (:conceptIds)
                  and l.deleted_at is null
            """,
            nativeQuery = true
    )
    List<Lesson> findAdminByConceptIds(@Param("conceptIds") List<Integer> conceptIds);

    @Query(
            value = """
                select l.*
                from lessons l 
                join lesson_concepts lc on lc.lesson_id = l.lesson_id
                where lc.concept_id in (:conceptIds)
                  and l.moderation_status = 'APPROVED'
                  and l.deleted_at is null
                group by l.lesson_id
                having count(distinct lc.concept_id) = :conceptCount
            """,
            nativeQuery = true
    )
    List<Lesson> findByAllConceptIds(
            @Param("conceptIds") List<Integer> conceptIds,
            @Param("conceptCount")  Integer conceptCount);

    @Query(
            value = """
                select l.*
                from lessons l
                join lesson_concepts lc on lc.lesson_id = l.lesson_id
                where lc.concept_id in (:conceptIds)
                  and l.deleted_at is null
                group by l.lesson_id
                having count(distinct lc.concept_id) = :conceptCount
            """,
            nativeQuery = true
    )
    List<Lesson> findAdminByAllConceptIds(
            @Param("conceptIds") List<Integer> conceptIds,
            @Param("conceptCount") Integer conceptCount
    );

    @Query(
            value = """
                select distinct l.*
                from lessons l
                join lesson_concepts lc on lc.lesson_id = l.lesson_id
                where lc.concept_id in (:conceptIds)
                  and l.moderation_status = cast(:status as lessons_moderation_status)
                  and l.deleted_at is null
            """,
            nativeQuery = true
    )
    List<Lesson> findAdminByConceptIdsAndStatus(
            @Param("conceptIds") List<Integer> conceptIds,
            @Param("status") String status
    );

    @Query(
            value = """
                select l.*
                from lessons l
                join lesson_concepts lc on lc.lesson_id = l.lesson_id
                where lc.concept_id in (:conceptIds)
                  and l.moderation_status = cast(:status as lessons_moderation_status)
                  and l.deleted_at is null
                group by l.lesson_id
                having count(distinct lc.concept_id) = :conceptCount
            """,
            nativeQuery = true
    )
    List<Lesson> findAdminByAllConceptIdsAndStatus(
            @Param("conceptIds") List<Integer> conceptIds,
            @Param("conceptCount") Integer conceptCount,
            @Param("status") String status
    );

    @Query(
            value = """
                select distinct l.*
                from lessons l
                join lesson_concepts lc on lc.lesson_id = l.lesson_id
                where l.contributor_id = :contributorId
                  and l.deleted_at is null
                  and lc.concept_id in (:conceptIds)
            """,
            nativeQuery = true
    )
    List<Lesson> findByContributorAndConceptIds(
            @Param("contributorId") UUID contributorId,
            @Param("conceptIds") List<Integer> conceptIds
    );

    @Query(
            value = """
                select l.*
                from lessons l
                join lesson_concepts lc on lc.lesson_id = l.lesson_id
                where l.contributor_id = :contributorId
                  and l.deleted_at is null
                  and lc.concept_id in (:conceptIds)
                group by l.lesson_id
                having count(distinct lc.concept_id) = :conceptCount
            """,
            nativeQuery = true
    )
    List<Lesson> findByContributorAndAllConceptIds(
            @Param("contributorId") UUID contributorId,
            @Param("conceptIds") List<Integer> conceptIds,
            @Param("conceptCount") Integer conceptCount
    );

    @Query("select count(distinct l.lessonId) from Lesson l join l.concepts c where c.conceptId = :conceptId")
    long countLinkedLessonsByConceptId(@Param("conceptId") Integer conceptId);

    @Modifying
    @Query(
            value = """
                    update lessons
                    set moderation_status = 'UNPUBLISHED'
                    where contributor_id = :contributorId
                      and deleted_at is null
                    """,
            nativeQuery = true
    )
    void unpublishByContributorId(@Param("contributorId") UUID contributorId);

    @Query(
            value = """
                    select l.*
                    from lessons l
                    where l.lesson_id = :lessonId
                      and l.moderation_status = 'APPROVED'
                      and l.deleted_at is null
                    """,
            nativeQuery = true
    )
    java.util.Optional<Lesson> findPublicById(@Param("lessonId") Integer lessonId);
}
