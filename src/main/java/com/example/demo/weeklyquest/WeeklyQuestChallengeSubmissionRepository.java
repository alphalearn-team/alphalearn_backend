package com.example.demo.weeklyquest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.weeklyquest.enums.SubmissionVisibility;

public interface WeeklyQuestChallengeSubmissionRepository extends JpaRepository<WeeklyQuestChallengeSubmission, Long> {

    Optional<WeeklyQuestChallengeSubmission> findByLearner_IdAndWeeklyQuestAssignment_Id(UUID learnerId, Long weeklyQuestAssignmentId);

    Optional<WeeklyQuestChallengeSubmission> findByLearner_IdAndWeeklyQuestAssignment_PublicId(UUID learnerId, UUID weeklyQuestAssignmentPublicId);

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                a.publicId as assignmentPublicId,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id <> :learnerId
              and exists (
                select 1
                from Friend f
                where (f.userId1 = :learnerId and f.userId2 = l.id)
                   or (f.userId2 = :learnerId and f.userId1 = l.id)
            )
            order by s.submittedAt desc, s.id desc
            """)
    Slice<FriendQuestChallengeFeedProjection> findFriendChallengeFeedByLearnerId(UUID learnerId, Pageable pageable);

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                a.publicId as assignmentPublicId,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id <> :learnerId
              and exists (
                select 1
                from Friend f
                where (f.userId1 = :learnerId and f.userId2 = l.id)
                   or (f.userId2 = :learnerId and f.userId1 = l.id)
            )
              and s.submittedAt >= :submittedFrom
              and s.submittedAt <= :submittedTo
            order by s.submittedAt desc, s.id desc
            """)
    Slice<FriendQuestChallengeFeedProjection> findFriendChallengeFeedByLearnerIdAndSubmittedAtRange(
            UUID learnerId,
            OffsetDateTime submittedFrom,
            OffsetDateTime submittedTo,
            Pageable pageable
    );

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                a.publicId as assignmentPublicId,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id <> :learnerId
              and exists (
                select 1
                from Friend f
                where (f.userId1 = :learnerId and f.userId2 = l.id)
                   or (f.userId2 = :learnerId and f.userId1 = l.id)
            )
              and w.publicId in :weekPublicIds
            order by s.submittedAt desc, s.id desc
            """)
    Slice<FriendQuestChallengeFeedProjection> findFriendChallengeFeedByLearnerIdAndWeekPublicIds(
            UUID learnerId,
            List<UUID> weekPublicIds,
            Pageable pageable
    );

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                a.publicId as assignmentPublicId,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id <> :learnerId
              and exists (
                select 1
                from Friend f
                where (f.userId1 = :learnerId and f.userId2 = l.id)
                   or (f.userId2 = :learnerId and f.userId1 = l.id)
            )
              and w.publicId in :weekPublicIds
              and s.submittedAt >= :submittedFrom
              and s.submittedAt <= :submittedTo
            order by s.submittedAt desc, s.id desc
            """)
    Slice<FriendQuestChallengeFeedProjection> findFriendChallengeFeedByLearnerIdAndWeekPublicIdsAndSubmittedAtRange(
            UUID learnerId,
            List<UUID> weekPublicIds,
            OffsetDateTime submittedFrom,
            OffsetDateTime submittedTo,
            Pageable pageable
    );

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                a.publicId as assignmentPublicId,
                w.publicId as weekPublicId,
                w.weekStartAt as weekStartAt,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt,
                s.visibility as visibility
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id = :learnerId
            order by s.submittedAt desc, s.id desc
            """)
    Slice<QuestHistoryProjection> findMyQuestHistory(UUID learnerId, Pageable pageable);

        @Query("""
          select
        s.publicId as submissionPublicId,
        l.publicId as learnerPublicId,
        l.username as learnerUsername,
        a.publicId as assignmentPublicId,
        w.publicId as weekPublicId,
        w.weekStartAt as weekStartAt,
        c.publicId as conceptPublicId,
        c.title as conceptTitle,
        s.mediaPublicUrl as mediaPublicUrl,
        s.mediaContentType as mediaContentType,
        s.originalFilename as originalFilename,
        s.caption as caption,
        s.submittedAt as submittedAt,
        s.visibility as visibility
          from WeeklyQuestChallengeSubmission s
          join s.learner l
          join s.weeklyQuestAssignment a
          join a.week w
          join a.concept c
          where l.id = :learnerId
            and s.submittedAt >= :submittedFrom
            and s.submittedAt <= :submittedTo
          order by s.submittedAt desc, s.id desc
          """)
            Slice<QuestHistoryProjection> findMyQuestHistoryBySubmittedAtRange(
          UUID learnerId,
          OffsetDateTime submittedFrom,
          OffsetDateTime submittedTo,
          Pageable pageable
        );

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                a.publicId as assignmentPublicId,
                w.publicId as weekPublicId,
                w.weekStartAt as weekStartAt,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt,
                s.visibility as visibility
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id = :learnerId
              and w.publicId in :weekPublicIds
            order by s.submittedAt desc, s.id desc
            """)
    Slice<QuestHistoryProjection> findMyQuestHistoryByWeekPublicIds(UUID learnerId, List<UUID> weekPublicIds, Pageable pageable);

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                a.publicId as assignmentPublicId,
                w.publicId as weekPublicId,
                w.weekStartAt as weekStartAt,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt,
                s.visibility as visibility
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id = :learnerId
              and w.publicId in :weekPublicIds
              and s.submittedAt >= :submittedFrom
              and s.submittedAt <= :submittedTo
            order by s.submittedAt desc, s.id desc
            """)
    Slice<QuestHistoryProjection> findMyQuestHistoryByWeekPublicIdsAndSubmittedAtRange(
            UUID learnerId,
            List<UUID> weekPublicIds,
            OffsetDateTime submittedFrom,
            OffsetDateTime submittedTo,
            Pageable pageable
    );

        @Query("""
            select
                                s.publicId as submissionPublicId,
                                l.publicId as learnerPublicId,
                                l.username as learnerUsername,
                                a.publicId as assignmentPublicId,
                                w.publicId as weekPublicId,
                                w.weekStartAt as weekStartAt,
                                c.publicId as conceptPublicId,
                                c.title as conceptTitle,
                                s.mediaPublicUrl as mediaPublicUrl,
                                s.mediaContentType as mediaContentType,
                                s.originalFilename as originalFilename,
                                s.caption as caption,
                                s.submittedAt as submittedAt,
                                s.visibility as visibility
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id = :friendLearnerId
              and s.visibility in :allowedVisibilities
            order by s.submittedAt desc, s.id desc
            """)
    Slice<QuestHistoryProjection> findFriendQuestHistory(
            UUID friendLearnerId,
            List<SubmissionVisibility> allowedVisibilities,
            Pageable pageable
    );

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                a.publicId as assignmentPublicId,
                w.publicId as weekPublicId,
                w.weekStartAt as weekStartAt,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt,
                s.visibility as visibility
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id = :friendLearnerId
              and s.visibility in :allowedVisibilities
              and s.submittedAt >= :submittedFrom
              and s.submittedAt <= :submittedTo
            order by s.submittedAt desc, s.id desc
            """)
    Slice<QuestHistoryProjection> findFriendQuestHistoryBySubmittedAtRange(
            UUID friendLearnerId,
            List<SubmissionVisibility> allowedVisibilities,
            OffsetDateTime submittedFrom,
            OffsetDateTime submittedTo,
            Pageable pageable
    );

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                a.publicId as assignmentPublicId,
                w.publicId as weekPublicId,
                w.weekStartAt as weekStartAt,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt,
                s.visibility as visibility
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id = :friendLearnerId
              and s.visibility in :allowedVisibilities
              and w.publicId in :weekPublicIds
            order by s.submittedAt desc, s.id desc
            """)
    Slice<QuestHistoryProjection> findFriendQuestHistoryByWeekPublicIds(
            UUID friendLearnerId,
            List<SubmissionVisibility> allowedVisibilities,
            List<UUID> weekPublicIds,
            Pageable pageable
    );

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                a.publicId as assignmentPublicId,
                w.publicId as weekPublicId,
                w.weekStartAt as weekStartAt,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt,
                s.visibility as visibility
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id = :friendLearnerId
              and s.visibility in :allowedVisibilities
              and w.publicId in :weekPublicIds
              and s.submittedAt >= :submittedFrom
              and s.submittedAt <= :submittedTo
            order by s.submittedAt desc, s.id desc
            """)
    Slice<QuestHistoryProjection> findFriendQuestHistoryByWeekPublicIdsAndSubmittedAtRange(
            UUID friendLearnerId,
            List<SubmissionVisibility> allowedVisibilities,
            List<UUID> weekPublicIds,
            OffsetDateTime submittedFrom,
            OffsetDateTime submittedTo,
            Pageable pageable
    );

    @Query("""
            select
                s.publicId as submissionPublicId,
                l.publicId as learnerPublicId,
                l.username as learnerUsername,
                a.publicId as assignmentPublicId,
                w.publicId as weekPublicId,
                w.weekStartAt as weekStartAt,
                c.publicId as conceptPublicId,
                c.title as conceptTitle,
                s.mediaPublicUrl as mediaPublicUrl,
                s.mediaContentType as mediaContentType,
                s.originalFilename as originalFilename,
                s.caption as caption,
                s.submittedAt as submittedAt,
                s.visibility as visibility
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id = :learnerId
              and s.visibility = :visibility
            order by s.submittedAt desc, s.id desc
            """)
        Slice<QuestHistoryProjection> findPublicQuestHistory(UUID learnerId, SubmissionVisibility visibility, Pageable pageable);

        @Query("""
            select
                                s.publicId as submissionPublicId,
                                l.publicId as learnerPublicId,
                                l.username as learnerUsername,
                                a.publicId as assignmentPublicId,
                                w.publicId as weekPublicId,
                                w.weekStartAt as weekStartAt,
                                c.publicId as conceptPublicId,
                                c.title as conceptTitle,
                                s.mediaPublicUrl as mediaPublicUrl,
                                s.mediaContentType as mediaContentType,
                                s.originalFilename as originalFilename,
                                s.caption as caption,
                                s.submittedAt as submittedAt,
                                s.visibility as visibility
            from WeeklyQuestChallengeSubmission s
            join s.learner l
            join s.weeklyQuestAssignment a
            join a.week w
            join a.concept c
            where l.id = :learnerId
              and s.visibility = :visibility
              and w.publicId in :weekPublicIds
            order by s.submittedAt desc, s.id desc
            """)
    Slice<QuestHistoryProjection> findPublicQuestHistoryByWeekPublicIds(
            UUID learnerId,
            SubmissionVisibility visibility,
            List<UUID> weekPublicIds,
            Pageable pageable
    );
}
