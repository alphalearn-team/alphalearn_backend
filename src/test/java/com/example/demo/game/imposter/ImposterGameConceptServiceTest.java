package com.example.demo.game.imposter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.game.imposter.dto.ImposterAssignedConceptDto;
import com.example.demo.game.imposter.dto.NextImposterConceptRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ImposterGameConceptServiceTest {

    @Mock
    private ConceptRepository conceptRepository;

    @Test
    void assignsRandomConceptOutsideExcludedSet() {
        Concept firstConcept = concept("alpha");
        Concept secondConcept = concept("beta");
        when(conceptRepository.findAll()).thenReturn(List.of(firstConcept, secondConcept));

        ImposterGameConceptService service = new ImposterGameConceptService(conceptRepository);

        ImposterAssignedConceptDto result = service.assignNextConcept(
                new NextImposterConceptRequest(List.of(firstConcept.getPublicId()))
        );

        assertThat(result.conceptPublicId()).isEqualTo(secondConcept.getPublicId());
        assertThat(result.word()).isEqualTo("beta");
    }

    @Test
    void rejectsWhenNoConceptsAreAvailable() {
        when(conceptRepository.findAll()).thenReturn(List.of());

        ImposterGameConceptService service = new ImposterGameConceptService(conceptRepository);

        assertThatThrownBy(() -> service.assignNextConcept(new NextImposterConceptRequest(List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No imposter game concepts are available");
    }

    @Test
    void rejectsWhenEveryConceptIsExcluded() {
        Concept onlyConcept = concept("solo");
        when(conceptRepository.findAll()).thenReturn(List.of(onlyConcept));

        ImposterGameConceptService service = new ImposterGameConceptService(conceptRepository);

        assertThatThrownBy(() -> service.assignNextConcept(
                new NextImposterConceptRequest(List.of(onlyConcept.getPublicId()))
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No imposter game concepts are available");
    }

    private Concept concept(String title) {
        Concept concept = new Concept();
        ReflectionTestUtils.setField(concept, "publicId", UUID.randomUUID());
        concept.setTitle(title);
        concept.setDescription(title + " description");
        concept.setCreatedAt(OffsetDateTime.parse("2026-03-01T00:00:00Z"));
        return concept;
    }
}
