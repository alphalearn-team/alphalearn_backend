package com.example.demo.admin.weeklyconcept;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.config.SupabaseAuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.weeklyconcept.WeeklyConcept;
import com.example.demo.weeklyconcept.WeeklyConceptRepository;
import com.example.demo.weeklyconcept.dto.WeeklyConceptResponse;
import com.example.demo.weeklyconcept.dto.WeeklyConceptUpsertRequest;

@Service
public class AdminWeeklyConceptService {

    private static final int MAX_CONCEPT_LENGTH = 500;

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

    @Transactional
    public WeeklyConceptResponse upsertByWeekStartDate(
            LocalDate weekStartDate,
            WeeklyConceptUpsertRequest request,
            SupabaseAuthUser user
    ) {
        UUID actorUserId = requireActorUserId(user);
        String concept = validateAndNormalizeConcept(request);
        OffsetDateTime now = OffsetDateTime.now();

        WeeklyConcept weeklyConcept = weeklyConceptRepository.findByWeekStartDate(weekStartDate)
                .orElseGet(WeeklyConcept::new);

        weeklyConcept.setWeekStartDate(weekStartDate);
        weeklyConcept.setConcept(concept);
        weeklyConcept.setUpdatedBy(actorUserId);
        weeklyConcept.setUpdatedAt(now);

        WeeklyConcept saved = weeklyConceptRepository.save(weeklyConcept);
        return new WeeklyConceptResponse(
                saved.getWeekStartDate(),
                saved.getConcept(),
                saved.getUpdatedAt()
        );
    }

    private String validateAndNormalizeConcept(WeeklyConceptUpsertRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        String concept = request.concept();
        if (concept == null || concept.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "concept is required");
        }

        String trimmed = concept.trim();
        if (trimmed.length() > MAX_CONCEPT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "concept must be at most " + MAX_CONCEPT_LENGTH + " characters"
            );
        }
        return trimmed;
    }

    private UUID requireActorUserId(SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated admin user required");
        }
        return user.userId();
    }
}
