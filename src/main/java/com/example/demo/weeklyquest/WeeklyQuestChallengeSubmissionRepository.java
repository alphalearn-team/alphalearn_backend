package com.example.demo.weeklyquest;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyQuestChallengeSubmissionRepository extends JpaRepository<WeeklyQuestChallengeSubmission, Long> {

    Optional<WeeklyQuestChallengeSubmission> findByLearner_IdAndWeeklyQuestAssignment_Id(UUID learnerId, Long weeklyQuestAssignmentId);

    Optional<WeeklyQuestChallengeSubmission> findByLearner_IdAndWeeklyQuestAssignment_PublicId(UUID learnerId, UUID weeklyQuestAssignmentPublicId);
}
