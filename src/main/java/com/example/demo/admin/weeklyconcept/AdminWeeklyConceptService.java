package com.example.demo.admin.weeklyconcept;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
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

    private final WeeklyConceptRepository weeklyConceptRepository;
    private final ConceptRepository conceptRepository;

    public AdminWeeklyConceptService(
            WeeklyConceptRepository weeklyConceptRepository,
            ConceptRepository conceptRepository
    ) {
        this.weeklyConceptRepository = weeklyConceptRepository;
        this.conceptRepository = conceptRepository;
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
                weeklyConcept.getConcept().getPublicId(),
                weeklyConcept.getConcept().getTitle(),
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
        Concept concept = validateAndResolveConcept(request);
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
                saved.getConcept().getPublicId(),
                saved.getConcept().getTitle(),
                saved.getUpdatedAt()
        );
    }

    private Concept validateAndResolveConcept(WeeklyConceptUpsertRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        UUID conceptPublicId = request.conceptPublicId();
        if (conceptPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conceptPublicId is required");
        }

        return conceptRepository.findByPublicId(conceptPublicId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Concept not found: " + conceptPublicId
                ));
    }

    private UUID requireActorUserId(SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated admin user required");
        }
        return user.userId();
    }
}
