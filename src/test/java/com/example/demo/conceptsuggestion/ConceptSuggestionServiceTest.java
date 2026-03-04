package com.example.demo.conceptsuggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.conceptsuggestion.dto.ConceptSuggestionDto;
import com.example.demo.conceptsuggestion.dto.SaveConceptSuggestionRequest;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;

@ExtendWith(MockitoExtension.class)
class ConceptSuggestionServiceTest {

    @Mock
    private ConceptSuggestionRepository conceptSuggestionRepository;

    @Mock
    private LearnerRepository learnerRepository;

    private ConceptSuggestionService conceptSuggestionService;

    @BeforeEach
    void setUp() {
        conceptSuggestionService = new ConceptSuggestionService(conceptSuggestionRepository, learnerRepository);
    }

    @Test
    void createDraftAssociatesAuthenticatedLearnerAndDefaultsToDraft() {
        UUID userId = UUID.randomUUID();
        Learner learner = learner(userId);
        SupabaseAuthUser user = authUser(userId, learner);

        when(learnerRepository.findById(userId)).thenReturn(Optional.of(learner));
        when(conceptSuggestionRepository.save(any(ConceptSuggestion.class))).thenAnswer(invocation -> {
            ConceptSuggestion suggestion = invocation.getArgument(0);
            suggestion.assignDefaultsIfMissing();
            return suggestion;
        });

        ConceptSuggestionDto result = conceptSuggestionService.createDraft(
                new SaveConceptSuggestionRequest("  Number bonds  ", "  Help children decompose numbers.  "),
                user
        );

        ArgumentCaptor<ConceptSuggestion> captor = ArgumentCaptor.forClass(ConceptSuggestion.class);
        verify(conceptSuggestionRepository).save(captor.capture());
        ConceptSuggestion saved = captor.getValue();

        assertThat(saved.getOwner()).isEqualTo(learner);
        assertThat(saved.getTitle()).isEqualTo("Number bonds");
        assertThat(saved.getDescription()).isEqualTo("Help children decompose numbers.");
        assertThat(result.status()).isEqualTo("DRAFT");
    }

    @Test
    void updateDraftPersistsEditedContentForOwner() {
        UUID ownerId = UUID.randomUUID();
        Learner learner = learner(ownerId);
        SupabaseAuthUser user = authUser(ownerId, learner);
        ConceptSuggestion suggestion = draftSuggestion(learner);
        UUID suggestionPublicId = suggestion.getPublicId();

        when(conceptSuggestionRepository.findByPublicId(suggestionPublicId)).thenReturn(Optional.of(suggestion));
        when(conceptSuggestionRepository.save(any(ConceptSuggestion.class))).thenAnswer(invocation -> {
            ConceptSuggestion saved = invocation.getArgument(0);
            saved.touchUpdatedAt();
            return saved;
        });

        ConceptSuggestionDto result = conceptSuggestionService.updateDraft(
                suggestionPublicId,
                new SaveConceptSuggestionRequest("Updated draft", "Updated description"),
                user
        );

        ArgumentCaptor<ConceptSuggestion> captor = ArgumentCaptor.forClass(ConceptSuggestion.class);
        verify(conceptSuggestionRepository).save(captor.capture());
        ConceptSuggestion saved = captor.getValue();

        assertThat(saved.getTitle()).isEqualTo("Updated draft");
        assertThat(saved.getDescription()).isEqualTo("Updated description");
        assertThat(result.title()).isEqualTo("Updated draft");
        assertThat(result.description()).isEqualTo("Updated description");
    }

    @Test
    void updateDraftRejectsNonOwners() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Learner owner = learner(ownerId);
        Learner otherLearner = learner(otherUserId);
        ConceptSuggestion suggestion = draftSuggestion(owner);

        when(conceptSuggestionRepository.findByPublicId(suggestion.getPublicId())).thenReturn(Optional.of(suggestion));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> conceptSuggestionService.updateDraft(
                        suggestion.getPublicId(),
                        new SaveConceptSuggestionRequest("Edited", "Edited"),
                        authUser(otherUserId, otherLearner)
                )
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void updateDraftRejectsNonDraftSuggestions() {
        UUID ownerId = UUID.randomUUID();
        Learner learner = learner(ownerId);
        ConceptSuggestion suggestion = draftSuggestion(learner);
        suggestion.setStatus(ConceptSuggestionStatus.SUBMITTED);

        when(conceptSuggestionRepository.findByPublicId(suggestion.getPublicId())).thenReturn(Optional.of(suggestion));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> conceptSuggestionService.updateDraft(
                        suggestion.getPublicId(),
                        new SaveConceptSuggestionRequest("Edited", "Edited"),
                        authUser(ownerId, learner)
                )
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
    }

    private SupabaseAuthUser authUser(UUID userId, Learner learner) {
        return new SupabaseAuthUser(userId, learner, null);
    }

    private Learner learner(UUID userId) {
        return new Learner(userId, UUID.randomUUID(), "user-" + userId, OffsetDateTime.now(), (short) 0);
    }

    private ConceptSuggestion draftSuggestion(Learner owner) {
        ConceptSuggestion suggestion = new ConceptSuggestion();
        suggestion.setOwner(owner);
        suggestion.setStatus(ConceptSuggestionStatus.DRAFT);
        suggestion.setTitle("Original title");
        suggestion.setDescription("Original description");
        suggestion.assignDefaultsIfMissing();
        return suggestion;
    }
}
