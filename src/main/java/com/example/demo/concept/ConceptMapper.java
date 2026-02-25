package com.example.demo.concept;

import org.springframework.stereotype.Component;

import com.example.demo.concept.dto.ConceptDTO;
import com.example.demo.concept.dto.ConceptPublicDto;

@Component
public class ConceptMapper {

    public ConceptDTO toDto(Concept concept) {
        return new ConceptDTO(
                concept.getConceptId(),
                concept.getTitle(),
                concept.getDescription(),
                concept.getCreatedAt()
        );
    }

    public ConceptPublicDto toPublicDto(Concept concept) {
        return new ConceptPublicDto(
                concept.getPublicId(),
                concept.getTitle(),
                concept.getDescription(),
                concept.getCreatedAt()
        );
    }
}
