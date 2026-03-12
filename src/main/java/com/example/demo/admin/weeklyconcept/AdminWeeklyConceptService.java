package com.example.demo.admin.weeklyconcept;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.weeklyconcept.WeeklyConcept;
import com.example.demo.weeklyconcept.WeeklyConceptRepository;
import com.example.demo.weeklyconcept.dto.WeeklyConceptResponse;

@Service
public class AdminWeeklyConceptService {

    private final WeeklyConceptRepository weeklyConceptRepository;

    public AdminWeeklyConceptService(WeeklyConceptRepository weeklyConceptRepository) {
        this.weeklyConceptRepository = weeklyConceptRepository;
    }

    @Transactional(readOnly = true)
    public WeeklyConceptResponse getByWeekStartDate(LocalDate weekStartDate) {
        WeeklyConcept weeklyConcept = weeklyConceptRepository.findByWeekStartDate(weekStartDate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Weekly concept not found for weekStartDate: " + weekStartDate
                ));

        return new WeeklyConceptResponse(
                weeklyConcept.getWeekStartDate(),
                weeklyConcept.getConcept(),
                weeklyConcept.getUpdatedAt()
        );
    }
}
