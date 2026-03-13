package com.example.demo.weeklyquest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyQuestWeekRepository extends JpaRepository<WeeklyQuestWeek, Long> {

    Optional<WeeklyQuestWeek> findByPublicId(UUID publicId);

    Optional<WeeklyQuestWeek> findByWeekStartAt(OffsetDateTime weekStartAt);

    List<WeeklyQuestWeek> findAllByOrderByWeekStartAtAsc();
}
