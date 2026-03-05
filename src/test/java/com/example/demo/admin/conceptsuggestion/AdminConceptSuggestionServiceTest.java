package com.example.demo.admin.conceptsuggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.conceptsuggestion.ConceptSuggestion;
import com.example.demo.conceptsuggestion.ConceptSuggestionRepository;
import com.example.demo.conceptsuggestion.ConceptSuggestionStatus;
import com.example.demo.learner.Learner;

@ExtendWith(MockitoExtension.class)
class AdminConceptSuggestionServiceTest {

    @Mock
    private ConceptSuggestionRepository conceptSuggestionRepository;

    private AdminConceptSuggestionService adminConceptSuggestionService;

    @BeforeEach
    void setUp() {
        adminConceptSuggestionService = new AdminConceptSuggestionService(conceptSuggestionRepository);
    }

    @Test
    void getSubmittedSuggestionsReturnsRepositoryRowsInQueueOrder() {
        ConceptSuggestion oldestSubmitted = submittedSuggestion("Oldest", OffsetDateTime.parse("2026-03-05T08:00:00Z"));
        ConceptSuggestion newestSubmitted = submittedSuggestion("Newest", OffsetDateTime.parse("2026-03-05T09:00:00Z"));

        when(conceptSuggestionRepository.findAllByStatusOrderByUpdatedAtAsc(ConceptSuggestionStatus.SUBMITTED))
                .thenReturn(List.of(oldestSubmitted, newestSubmitted));

        List<AdminConceptSuggestionQueueItemDto> result = adminConceptSuggestionService.getSubmittedSuggestions();

        verify(conceptSuggestionRepository).findAllByStatusOrderByUpdatedAtAsc(ConceptSuggestionStatus.SUBMITTED);
        assertThat(result)
                .extracting(AdminConceptSuggestionQueueItemDto::title)
                .containsExactly("Oldest", "Newest");
        assertThat(result)
                .extracting(AdminConceptSuggestionQueueItemDto::status)
                .containsOnly("SUBMITTED");
    }

    @Test
    void getSubmittedSuggestionsMapsOwnerFieldsAndSubmissionTimestamp() {
        OffsetDateTime submittedAt = OffsetDateTime.parse("2026-03-05T10:15:30Z");
        ConceptSuggestion suggestion = submittedSuggestion("Queue item", submittedAt);

        when(conceptSuggestionRepository.findAllByStatusOrderByUpdatedAtAsc(ConceptSuggestionStatus.SUBMITTED))
                .thenReturn(List.of(suggestion));

        AdminConceptSuggestionQueueItemDto result = adminConceptSuggestionService.getSubmittedSuggestions().getFirst();

        assertThat(result.publicId()).isEqualTo(suggestion.getPublicId());
        assertThat(result.ownerPublicId()).isEqualTo(suggestion.getOwner().getPublicId());
        assertThat(result.ownerUsername()).isEqualTo(suggestion.getOwner().getUsername());
        assertThat(result.createdAt()).isEqualTo(suggestion.getCreatedAt());
        assertThat(result.submittedAt()).isEqualTo(submittedAt);
    }

    private ConceptSuggestion submittedSuggestion(String title, OffsetDateTime submittedAt) {
        Learner owner = new Learner(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "learner-" + title.toLowerCase(),
                OffsetDateTime.parse("2026-03-01T09:00:00Z"),
                (short) 0
        );

        return new ConceptSuggestion(
                null,
                UUID.randomUUID(),
                owner,
                title,
                title + " description",
                ConceptSuggestionStatus.SUBMITTED,
                OffsetDateTime.parse("2026-03-02T10:00:00Z"),
                submittedAt
        );
    }
}
