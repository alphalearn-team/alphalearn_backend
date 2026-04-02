package com.example.demo.admin.imposter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.admin.imposter.dto.AdminImposterMonthlyPackDto;
import com.example.demo.admin.imposter.dto.UpsertAdminImposterMonthlyPackRequest;
import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackConcept;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackWeeklyFeature;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackWeeklyFeatureRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminImposterMonthlyPackServiceTest {

    @Mock
    private ImposterMonthlyPackRepository imposterMonthlyPackRepository;

    @Mock
    private ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;

    @Mock
    private ImposterMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;

    @Mock
    private ConceptRepository conceptRepository;

    private AdminImposterMonthlyPackService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-02T00:00:00Z"), ZoneOffset.UTC);
        service = new AdminImposterMonthlyPackService(
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                imposterMonthlyPackWeeklyFeatureRepository,
                conceptRepository,
                fixedClock
        );
    }

    @Test
    void returnsScaffoldWhenPackDoesNotExist() {
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.empty());

        AdminImposterMonthlyPackDto result = service.getMonthlyPack("2026-04");

        assertThat(result.yearMonth()).isEqualTo("2026-04");
        assertThat(result.exists()).isFalse();
        assertThat(result.concepts()).isEmpty();
        assertThat(result.weeklyFeaturedConceptPublicIds()).isEmpty();
    }

    @Test
    void returnsOrderedConceptsAndWeeklyFeaturesWhenPackExists() {
        ImposterMonthlyPack pack = pack(88L, "2026-04");
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));

        Concept conceptOne = concept("alpha");
        Concept conceptTwo = concept("beta");
        when(imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(88L)).thenReturn(List.of(
                conceptRow(pack, conceptOne, (short) 1),
                conceptRow(pack, conceptTwo, (short) 2)
        ));

        when(imposterMonthlyPackWeeklyFeatureRepository.findByPack_IdOrderByWeekSlotAsc(88L)).thenReturn(List.of(
                weeklyFeature(pack, conceptTwo, (short) 1),
                weeklyFeature(pack, conceptOne, (short) 2)
        ));

        AdminImposterMonthlyPackDto result = service.getMonthlyPack("2026-04");

        assertThat(result.exists()).isTrue();
        assertThat(result.concepts()).hasSize(2);
        assertThat(result.concepts().get(0).slotIndex()).isEqualTo((short) 1);
        assertThat(result.concepts().get(0).conceptPublicId()).isEqualTo(conceptOne.getPublicId());
        assertThat(result.concepts().get(1).slotIndex()).isEqualTo((short) 2);
        assertThat(result.concepts().get(1).conceptPublicId()).isEqualTo(conceptTwo.getPublicId());
        assertThat(result.weeklyFeaturedConceptPublicIds()).containsExactly(
                conceptTwo.getPublicId(),
                conceptOne.getPublicId()
        );
    }

    @Test
    void rejectsWhenConceptCountIsNotTwenty() {
        UpsertAdminImposterMonthlyPackRequest request = new UpsertAdminImposterMonthlyPackRequest(
                randomIds(19),
                randomIds(4)
        );

        assertThatThrownBy(() -> service.upsertMonthlyPack("2026-04", request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenConceptsContainDuplicates() {
        List<UUID> conceptIds = randomIds(20);
        conceptIds.set(19, conceptIds.get(0));
        UpsertAdminImposterMonthlyPackRequest request = new UpsertAdminImposterMonthlyPackRequest(
                conceptIds,
                conceptIds.subList(0, 4)
        );

        assertThatThrownBy(() -> service.upsertMonthlyPack("2026-04", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("conceptPublicIds must contain unique values");
    }

    @Test
    void rejectsWhenWeeklyFeatureCountIsNotFour() {
        List<UUID> conceptIds = randomIds(20);
        UpsertAdminImposterMonthlyPackRequest request = new UpsertAdminImposterMonthlyPackRequest(
                conceptIds,
                conceptIds.subList(0, 3)
        );

        assertThatThrownBy(() -> service.upsertMonthlyPack("2026-04", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("weeklyFeaturedConceptPublicIds must contain exactly 4 items");
    }

    @Test
    void rejectsWhenWeeklyFeatureContainsConceptOutsidePack() {
        List<UUID> conceptIds = randomIds(20);
        List<UUID> featuredIds = new ArrayList<>(conceptIds.subList(0, 3));
        featuredIds.add(UUID.randomUUID());

        UpsertAdminImposterMonthlyPackRequest request = new UpsertAdminImposterMonthlyPackRequest(conceptIds, featuredIds);

        assertThatThrownBy(() -> service.upsertMonthlyPack("2026-04", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must be selected from conceptPublicIds");
    }

    @Test
    void rejectsWhenAtLeastOneConceptIsMissing() {
        List<UUID> conceptIds = randomIds(20);
        List<UUID> featuredIds = conceptIds.subList(0, 4);

        List<Concept> resolvedConcepts = conceptIds.stream()
                .limit(19)
                .map(this::conceptWithPublicId)
                .toList();
        when(conceptRepository.findAllByPublicIdIn(conceptIds)).thenReturn(resolvedConcepts);

        UpsertAdminImposterMonthlyPackRequest request = new UpsertAdminImposterMonthlyPackRequest(conceptIds, featuredIds);

        assertThatThrownBy(() -> service.upsertMonthlyPack("2026-04", request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @SuppressWarnings("unchecked")
    void upsertReplacesRowsAndPreservesSlotOrdering() {
        List<UUID> conceptIds = randomIds(20);
        List<UUID> featuredIds = List.of(conceptIds.get(19), conceptIds.get(0), conceptIds.get(5), conceptIds.get(9));

        List<Concept> resolvedConcepts = conceptIds.stream()
                .map(this::conceptWithPublicId)
                .collect(java.util.stream.Collectors.toList());
        Collections.reverse(resolvedConcepts);
        when(conceptRepository.findAllByPublicIdIn(conceptIds)).thenReturn(resolvedConcepts);

        ImposterMonthlyPack existingPack = pack(300L, "2026-04");
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(existingPack));

        UpsertAdminImposterMonthlyPackRequest request = new UpsertAdminImposterMonthlyPackRequest(conceptIds, featuredIds);

        AdminImposterMonthlyPackDto result = service.upsertMonthlyPack("2026-04", request);

        verify(imposterMonthlyPackConceptRepository).deleteByPack_Id(300L);
        verify(imposterMonthlyPackWeeklyFeatureRepository).deleteByPack_Id(300L);

        ArgumentCaptor<List<ImposterMonthlyPackConcept>> conceptRowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(imposterMonthlyPackConceptRepository).saveAll(conceptRowsCaptor.capture());
        List<ImposterMonthlyPackConcept> savedConceptRows = conceptRowsCaptor.getValue();

        assertThat(savedConceptRows).hasSize(20);
        for (int i = 0; i < 20; i++) {
            assertThat(savedConceptRows.get(i).getSlotIndex()).isEqualTo((short) (i + 1));
            assertThat(savedConceptRows.get(i).getConcept().getPublicId()).isEqualTo(conceptIds.get(i));
        }

        ArgumentCaptor<List<ImposterMonthlyPackWeeklyFeature>> featuredRowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(imposterMonthlyPackWeeklyFeatureRepository).saveAll(featuredRowsCaptor.capture());
        List<ImposterMonthlyPackWeeklyFeature> savedFeatureRows = featuredRowsCaptor.getValue();

        assertThat(savedFeatureRows).hasSize(4);
        for (int i = 0; i < 4; i++) {
            assertThat(savedFeatureRows.get(i).getWeekSlot()).isEqualTo((short) (i + 1));
            assertThat(savedFeatureRows.get(i).getConcept().getPublicId()).isEqualTo(featuredIds.get(i));
        }

        assertThat(result.exists()).isTrue();
        assertThat(result.yearMonth()).isEqualTo("2026-04");
        assertThat(result.concepts()).hasSize(20);
        assertThat(result.weeklyFeaturedConceptPublicIds()).containsExactlyElementsOf(featuredIds);
    }

    @Test
    void derivesCurrentMonthFromClock() {
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.empty());

        AdminImposterMonthlyPackDto result = service.getCurrentMonthlyPack();

        assertThat(result.yearMonth()).isEqualTo("2026-04");
    }

    private List<UUID> randomIds(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> UUID.randomUUID())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private ImposterMonthlyPack pack(Long id, String yearMonth) {
        ImposterMonthlyPack pack = new ImposterMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", id);
        pack.setYearMonth(yearMonth);
        pack.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        return pack;
    }

    private Concept concept(String title) {
        Concept concept = new Concept();
        ReflectionTestUtils.setField(concept, "publicId", UUID.randomUUID());
        concept.setTitle(title);
        concept.setDescription(title + " description");
        concept.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        return concept;
    }

    private Concept conceptWithPublicId(UUID publicId) {
        Concept concept = concept("concept-" + publicId.toString().substring(0, 8));
        ReflectionTestUtils.setField(concept, "publicId", publicId);
        return concept;
    }

    private ImposterMonthlyPackConcept conceptRow(ImposterMonthlyPack pack, Concept concept, short slotIndex) {
        ImposterMonthlyPackConcept row = new ImposterMonthlyPackConcept();
        row.setPack(pack);
        row.setConcept(concept);
        row.setSlotIndex(slotIndex);
        return row;
    }

    private ImposterMonthlyPackWeeklyFeature weeklyFeature(ImposterMonthlyPack pack, Concept concept, short weekSlot) {
        ImposterMonthlyPackWeeklyFeature row = new ImposterMonthlyPackWeeklyFeature();
        row.setPack(pack);
        row.setConcept(concept);
        row.setWeekSlot(weekSlot);
        return row;
    }
}
