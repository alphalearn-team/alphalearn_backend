package com.example.demo.weeklyquest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestTemplateRepository extends JpaRepository<QuestTemplate, Long> {

    Optional<QuestTemplate> findByPublicId(UUID publicId);

    List<QuestTemplate> findByActiveTrueOrderByTitleAsc();
}
