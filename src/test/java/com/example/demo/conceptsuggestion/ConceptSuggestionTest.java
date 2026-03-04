package com.example.demo.conceptsuggestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class ConceptSuggestionTest {

    @Test
    void assignDefaultsIfMissingInitializesDraftFields() {
        ConceptSuggestion suggestion = new ConceptSuggestion();

        suggestion.assignDefaultsIfMissing();

        assertThat(suggestion.getPublicId()).isNotNull();
        assertThat(suggestion.getStatus()).isEqualTo(ConceptSuggestionStatus.DRAFT);
        assertThat(suggestion.getCreatedAt()).isNotNull();
        assertThat(suggestion.getUpdatedAt()).isNotNull();
    }

    @Test
    void assignDefaultsIfMissingPreservesExistingCreatedAt() {
        ConceptSuggestion suggestion = new ConceptSuggestion();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-04T10:15:30+08:00");
        suggestion.setStatus(ConceptSuggestionStatus.SUBMITTED);
        suggestion.setCreatedAt(createdAt);

        suggestion.assignDefaultsIfMissing();

        assertThat(suggestion.getStatus()).isEqualTo(ConceptSuggestionStatus.SUBMITTED);
        assertThat(suggestion.getCreatedAt()).isEqualTo(createdAt);
        assertThat(suggestion.getUpdatedAt()).isNotNull();
    }

    @Test
    void touchUpdatedAtRefreshesTimestamp() {
        ConceptSuggestion suggestion = new ConceptSuggestion();
        OffsetDateTime initialUpdatedAt = OffsetDateTime.parse("2026-03-04T10:15:30+08:00");
        suggestion.setUpdatedAt(initialUpdatedAt);

        suggestion.touchUpdatedAt();

        assertThat(suggestion.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
