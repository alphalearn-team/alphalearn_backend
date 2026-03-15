package com.example.demo.lesson;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
  interface ConceptLessonCountView {
    UUID getConceptPublicId();

    String getConceptTitle();

    long getLessonCount();
  }

        java.util.Optional<Lesson> findByPublicId(UUID lessonPublicId);

  long countByDeletedAtIsNull();

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

                    @Query("""
                        select c.publicId as conceptPublicId,
                               c.title as conceptTitle,
                               count(distinct l.lessonId) as lessonCount
                        from Lesson l
                        join l.concepts c
                        where l.deletedAt is null
                        group by c.conceptId, c.publicId, c.title
                        order by count(distinct l.lessonId) desc, c.title asc
                        """)
                    List<ConceptLessonCountView> findTopConceptsByLessonCount(Pageable pageable);
}
