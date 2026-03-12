package com.example.demo.admin.weeklyconcept;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.config.SupabaseAuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.weeklyconcept.WeeklyConcept;
import com.example.demo.weeklyconcept.WeeklyConceptRepository;
import com.example.demo.weeklyconcept.dto.WeeklyConceptResponse;
import com.example.demo.weeklyconcept.dto.WeeklyConceptUpsertRequest;

@ExtendWith(MockitoExtension.class)
class AdminWeeklyConceptServiceTest {

    @Mock
    private WeeklyConceptRepository weeklyConceptRepository;

    @Mock
    private ConceptRepository conceptRepository;

    private AdminWeeklyConceptService service;

    @BeforeEach
    void setUp() {
        service = new AdminWeeklyConceptService(weeklyConceptRepository, conceptRepository);
    }

    @Test
    void getByWeekStartDateReturnsWeeklyConcept() {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-03-12T10:00:00Z");
        Concept concept = concept(UUID.randomUUID(), "Algebra foundations");

        WeeklyConcept weeklyConcept = new WeeklyConcept();
        weeklyConcept.setWeekStartDate(weekStartDate);
        weeklyConcept.setConcept(concept);
        weeklyConcept.setUpdatedBy(UUID.randomUUID());
        weeklyConcept.setUpdatedAt(updatedAt);

        when(weeklyConceptRepository.findByWeekStartDate(weekStartDate)).thenReturn(Optional.of(weeklyConcept));

        WeeklyConceptResponse response = service.getByWeekStartDate(weekStartDate);

        assertThat(response.weekStartDate()).isEqualTo(weekStartDate);
        assertThat(response.conceptPublicId()).isEqualTo(concept.getPublicId());
        assertThat(response.conceptTitle()).isEqualTo("Algebra foundations");
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void getByWeekStartDateThrowsNotFoundWhenMissing() {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");
        when(weeklyConceptRepository.findByWeekStartDate(weekStartDate)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getByWeekStartDate(weekStartDate)
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("Weekly concept not found for weekStartDate: 2026-03-09");
    }

    @Test
    void upsertByWeekStartDateCreatesNewRowAndSetsAuditFields() {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");
        UUID actorUserId = UUID.randomUUID();
        UUID conceptPublicId = UUID.randomUUID();
        SupabaseAuthUser user = new SupabaseAuthUser(actorUserId, null, null);
        Concept concept = concept(conceptPublicId, "Algebra foundations");

        when(weeklyConceptRepository.findByWeekStartDate(weekStartDate)).thenReturn(Optional.empty());
        when(conceptRepository.findByPublicId(conceptPublicId)).thenReturn(Optional.of(concept));
        when(weeklyConceptRepository.save(any(WeeklyConcept.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WeeklyConceptResponse response = service.upsertByWeekStartDate(
                weekStartDate,
                new WeeklyConceptUpsertRequest(conceptPublicId),
                user
        );

        ArgumentCaptor<WeeklyConcept> captor = ArgumentCaptor.forClass(WeeklyConcept.class);
        org.mockito.Mockito.verify(weeklyConceptRepository).save(captor.capture());
        WeeklyConcept saved = captor.getValue();

        assertThat(saved.getWeekStartDate()).isEqualTo(weekStartDate);
        assertThat(saved.getConcept()).isEqualTo(concept);
        assertThat(saved.getUpdatedBy()).isEqualTo(actorUserId);
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(response.weekStartDate()).isEqualTo(weekStartDate);
        assertThat(response.conceptPublicId()).isEqualTo(conceptPublicId);
        assertThat(response.conceptTitle()).isEqualTo("Algebra foundations");
        assertThat(response.updatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    void upsertByWeekStartDateUpdatesExistingRow() {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");
        UUID actorUserId = UUID.randomUUID();
        UUID conceptPublicId = UUID.randomUUID();
        SupabaseAuthUser user = new SupabaseAuthUser(actorUserId, null, null);
        Concept oldConcept = concept(UUID.randomUUID(), "Old concept");
        Concept newConcept = concept(conceptPublicId, "New concept");

        WeeklyConcept existing = new WeeklyConcept();
        existing.setWeekStartDate(weekStartDate);
        existing.setConcept(oldConcept);
        existing.setUpdatedBy(UUID.randomUUID());
        existing.setUpdatedAt(OffsetDateTime.parse("2026-03-01T10:00:00Z"));

        when(weeklyConceptRepository.findByWeekStartDate(weekStartDate)).thenReturn(Optional.of(existing));
        when(conceptRepository.findByPublicId(conceptPublicId)).thenReturn(Optional.of(newConcept));
        when(weeklyConceptRepository.save(any(WeeklyConcept.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WeeklyConceptResponse response = service.upsertByWeekStartDate(
                weekStartDate,
                new WeeklyConceptUpsertRequest(conceptPublicId),
                user
        );

        assertThat(existing.getConcept()).isEqualTo(newConcept);
        assertThat(existing.getUpdatedBy()).isEqualTo(actorUserId);
        assertThat(existing.getUpdatedAt()).isNotNull();

        assertThat(response.conceptPublicId()).isEqualTo(conceptPublicId);
        assertThat(response.conceptTitle()).isEqualTo("New concept");
        assertThat(response.updatedAt()).isEqualTo(existing.getUpdatedAt());
    }

    @Test
    void upsertByWeekStartDateRejectsMissingConceptPublicId() {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");
        SupabaseAuthUser user = new SupabaseAuthUser(UUID.randomUUID(), null, null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.upsertByWeekStartDate(weekStartDate, new WeeklyConceptUpsertRequest(null), user)
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).isEqualTo("conceptPublicId is required");
    }

    @Test
    void upsertByWeekStartDateRejectsUnknownConceptPublicId() {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");
        UUID conceptPublicId = UUID.randomUUID();
        SupabaseAuthUser user = new SupabaseAuthUser(UUID.randomUUID(), null, null);
        when(conceptRepository.findByPublicId(conceptPublicId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.upsertByWeekStartDate(weekStartDate, new WeeklyConceptUpsertRequest(conceptPublicId), user)
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("Concept not found: " + conceptPublicId);
    }

    @Test
    void upsertByWeekStartDateRejectsMissingActorUserId() {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.upsertByWeekStartDate(
                        weekStartDate,
                        new WeeklyConceptUpsertRequest(UUID.randomUUID()),
                        new SupabaseAuthUser(null, null, null)
                )
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(403);
        assertThat(ex.getReason()).isEqualTo("Authenticated admin user required");
    }

    private Concept concept(UUID publicId, String title) {
        return new Concept(
                1,
                publicId,
                title,
                title + " description",
                OffsetDateTime.parse("2026-01-01T00:00:00Z")
        );
    }
}
