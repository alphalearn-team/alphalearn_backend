package com.example.demo.lesson;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
        java.util.Optional<Lesson> findByPublicId(UUID lessonPublicId);

        @Query("""
                        select count(distinct l.lessonId)
                        from Lesson l
                        join l.concepts c
                        where c.conceptId = :conceptId
                          and l.deletedAt is null
                        """)
        long countLinkedLessonsByConceptId(@Param("conceptId") Integer conceptId);

        @Modifying
        @Query(value = """
                        update lessons
                        set moderation_status = 'UNPUBLISHED'
                        where contributor_id = :contributorId
                          and deleted_at is null
                        """, nativeQuery = true)
        void unpublishByContributorId(@Param("contributorId") UUID contributorId);

        @Query(value = """
                        select l.*
                        from lessons l
                        where l.public_id = :publicId
                          and l.moderation_status = 'APPROVED'
                          and l.deleted_at is null
                        """, nativeQuery = true)
        java.util.Optional<Lesson> findPublicByPublicId(@Param("publicId") UUID lessonPublicId);
}
