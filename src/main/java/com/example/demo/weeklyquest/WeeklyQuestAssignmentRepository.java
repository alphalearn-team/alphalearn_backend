package com.example.demo.weeklyquest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyQuestAssignmentRepository extends JpaRepository<WeeklyQuestAssignment, Long> {

    Optional<WeeklyQuestAssignment> findByWeek_IdAndOfficialTrue(Long weekId);

    List<WeeklyQuestAssignment> findByWeek_IdOrderBySlotIndexAsc(Long weekId);
}
