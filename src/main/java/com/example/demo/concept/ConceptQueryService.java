package com.example.demo.concept;

import java.util.List;

import com.example.demo.concept.dto.ConceptDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConceptQueryService {

    private final ConceptRepository conceptRepository;
    private final ConceptMapper conceptMapper;

    public ConceptQueryService(ConceptRepository conceptRepository, ConceptMapper conceptMapper) {
        this.conceptRepository = conceptRepository;
        this.conceptMapper = conceptMapper;
    }

    public List<ConceptDTO> getAllConcepts() {
        return conceptRepository.findAll()
                .stream()
                .map(conceptMapper::toDto)
                .toList();
    }

    public ConceptDTO getConceptById(Integer id) {
        Concept concept = conceptRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept not found: " + id));
        return conceptMapper.toDto(concept);
    }
}
