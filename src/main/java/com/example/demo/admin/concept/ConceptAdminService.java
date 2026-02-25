package com.example.demo.admin.concept;

import java.time.OffsetDateTime;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptMapper;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.concept.dto.ConceptCreateDTO;
import com.example.demo.concept.dto.ConceptDTO;
import com.example.demo.lesson.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConceptAdminService {

    private final ConceptRepository conceptRepository;
    private final LessonRepository lessonRepository;
    private final ConceptMapper conceptMapper;

    public ConceptAdminService(
            ConceptRepository conceptRepository,
            LessonRepository lessonRepository,
            ConceptMapper conceptMapper
    ) {
        this.conceptRepository = conceptRepository;
        this.lessonRepository = lessonRepository;
        this.conceptMapper = conceptMapper;
    }

    @Transactional
    public ConceptDTO createConcept(ConceptCreateDTO request) {
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
        return conceptMapper.toDto(saved);
    }

    @Transactional
    public ConceptDTO updateConcept(Integer id, Concept updatedConcept) {
        if (updatedConcept == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        Concept existing = conceptRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept not found: " + id));

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
        return conceptMapper.toDto(saved);
    }

    @Transactional
    public void deleteConcept(Integer id) {
        Concept concept = conceptRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept not found: " + id));

        long linkedLessons = lessonRepository.countLinkedLessonsByConceptId(id);
        if (linkedLessons > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete concept " + id + " because it is used by " + linkedLessons
                            + " lesson(s). Remove the concept from those lessons first."
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
