package com.example.demo.concept.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptMapper;
import com.example.demo.concept.ConceptQueryService;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.concept.dto.ConceptCreateDTO;
import com.example.demo.concept.dto.ConceptPublicDto;
import com.example.demo.lesson.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminConceptService {

    private final ConceptRepository conceptRepository;
    private final LessonRepository lessonRepository;
    private final ConceptMapper conceptMapper;
    private final ConceptQueryService conceptQueryService;

    public AdminConceptService(
            ConceptRepository conceptRepository,
            LessonRepository lessonRepository,
            ConceptMapper conceptMapper,
            ConceptQueryService conceptQueryService
    ) {
        this.conceptRepository = conceptRepository;
        this.lessonRepository = lessonRepository;
        this.conceptMapper = conceptMapper;
        this.conceptQueryService = conceptQueryService;
    }

    @Transactional(readOnly = true)
    public java.util.List<ConceptPublicDto> getAllConcepts() {
        return conceptQueryService.getAllPublicConcepts();
    }

    @Transactional(readOnly = true)
    public ConceptPublicDto getConceptByPublicId(UUID conceptPublicId) {
        return conceptQueryService.getConceptByPublicId(conceptPublicId);
    }

    @Transactional
    public ConceptPublicDto createConcept(ConceptCreateDTO request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        String title = trimToNull(request.title());
        String description = trimToNull(request.description());
        if (title == null || description == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title and description are required");
        }

        Concept concept = new Concept();
        concept.setTitle(title);
        concept.setDescription(description);
        concept.setCreatedAt(OffsetDateTime.now());

        Concept saved = conceptRepository.save(concept);
        return conceptMapper.toPublicDto(saved);
    }

    @Transactional
    public ConceptPublicDto updateConcept(UUID conceptPublicId, Concept updatedConcept) {
        if (updatedConcept == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        Concept existing = conceptRepository.findByPublicId(conceptPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept not found: " + conceptPublicId));

        String title = trimToNull(updatedConcept.getTitle());
        String description = trimToNull(updatedConcept.getDescription());
        if (title == null || description == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "title and description are required"
            );
        }

        existing.setTitle(title);
        existing.setDescription(description);

        Concept saved = conceptRepository.save(existing);
        return conceptMapper.toPublicDto(saved);
    }

    @Transactional
    public void deleteConcept(UUID conceptPublicId) {
        Concept concept = conceptRepository.findByPublicId(conceptPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept not found: " + conceptPublicId));
        Integer conceptId = concept.getConceptId();

        long linkedLessons = lessonRepository.countLinkedLessonsByConceptId(conceptId);
        if (linkedLessons > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete concept: lessons are attached"
            );
        }

        conceptRepository.delete(concept);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
