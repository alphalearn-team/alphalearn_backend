package com.example.demo.weeklyconcept;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyConceptRepository extends JpaRepository<WeeklyConcept, Long> {

    Optional<WeeklyConcept> findByWeekStartDate(LocalDate weekStartDate);
}
