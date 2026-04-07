package com.example.demo.lesson.report;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonReportRepository extends JpaRepository<LessonReport, Long> {

    interface PendingLessonAggregateProjection {
        UUID getLessonPublicId();

        String getTitle();

        UUID getAuthorPublicId();

        String getAuthorUsername();

        String getLessonModerationStatus();

        long getPendingReportCount();

        String getLatestReason();

        java.time.Instant getLatestReportedAt();
    }

    boolean existsByLesson_LessonIdAndReporterUserId(Integer lessonId, UUID reporterUserId);

    List<LessonReport> findByLesson_PublicIdAndStatusOrderByCreatedAtDesc(UUID lessonPublicId, LessonReportStatus status);

    @Modifying
    @Query("""
            update LessonReport lr
            set lr.status = :resolvedStatus,
                lr.resolvedAt = :resolvedAt,
                lr.resolvedByAdminUserId = :resolvedByAdminUserId,
                lr.resolutionAction = :resolutionAction
            where lr.lesson.lessonId = :lessonId
              and lr.status = :pendingStatus
            """)
    int resolvePendingForLessonId(
            @Param("lessonId") Integer lessonId,
            @Param("pendingStatus") LessonReportStatus pendingStatus,
            @Param("resolvedStatus") LessonReportStatus resolvedStatus,
            @Param("resolvedAt") java.time.OffsetDateTime resolvedAt,
            @Param("resolvedByAdminUserId") UUID resolvedByAdminUserId,
            @Param("resolutionAction") LessonReportResolutionAction resolutionAction
    );

    @Modifying
    @Query("""
            update LessonReport lr
            set lr.status = :resolvedStatus,
                lr.resolvedAt = :resolvedAt,
                lr.resolvedByAdminUserId = :resolvedByAdminUserId,
                lr.resolutionAction = :resolutionAction
            where lr.publicId = :reportPublicId
              and lr.status = :pendingStatus
            """)
    int resolvePendingByReportPublicId(
            @Param("reportPublicId") UUID reportPublicId,
            @Param("pendingStatus") LessonReportStatus pendingStatus,
            @Param("resolvedStatus") LessonReportStatus resolvedStatus,
            @Param("resolvedAt") java.time.OffsetDateTime resolvedAt,
            @Param("resolvedByAdminUserId") UUID resolvedByAdminUserId,
            @Param("resolutionAction") LessonReportResolutionAction resolutionAction
    );

    @Query(value = """
            select
                l.public_id as lessonPublicId,
                l.title as title,
                lrn.public_id as authorPublicId,
                lrn.username as authorUsername,
                l.moderation_status::text as lessonModerationStatus,
                agg.pending_report_count as pendingReportCount,
                latest.reason as latestReason,
                latest.created_at as latestReportedAt
            from public.lessons l
            join (
                select lesson_id, count(*) as pending_report_count
                from public.lesson_reports
                where status = 'PENDING'
                group by lesson_id
            ) agg on agg.lesson_id = l.lesson_id
            join lateral (
                select r.reason, r.created_at
                from public.lesson_reports r
                where r.lesson_id = l.lesson_id
                  and r.status = 'PENDING'
                order by r.created_at desc, r.lesson_report_id desc
                limit 1
            ) latest on true
            left join public.contributors c on c.contributor_id = l.contributor_id
            left join public.learners lrn on lrn.id = c.contributor_id
            where l.deleted_at is null
            order by latest.created_at desc, l.created_at desc
            """, nativeQuery = true)
    List<PendingLessonAggregateProjection> findPendingLessonAggregates();
}
