package com.example.demo.weeklyquest;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
