package com.example.demo.me.imposter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.concept.Concept;
import com.example.demo.game.imposter.ImposterWeeklyFeaturedConceptService;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackConcept;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackWeeklyFeature;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackWeeklyFeatureRepository;
import com.example.demo.learner.Learner;
import com.example.demo.me.imposter.dto.LearnerCurrentImposterMonthlyPackDto;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LearnerImposterMonthlyPackServiceTest {

    @Mock
    private ImposterMonthlyPackRepository imposterMonthlyPackRepository;

    @Mock
    private ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;

    @Mock
    private ImposterMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;

    @Mock
    private ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;

    private LearnerImposterMonthlyPackService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-15T00:00:00Z"), ZoneOffset.UTC);
        service = new LearnerImposterMonthlyPackService(
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                imposterMonthlyPackWeeklyFeatureRepository,
                imposterWeeklyFeaturedConceptService,
                clock
        );
    }

    @Test
    void returnsExistsFalseWhenCurrentMonthPackMissing() {
        when(imposterWeeklyFeaturedConceptService.currentWeekSlotInMonth()).thenReturn((short) 2);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.empty());

        LearnerCurrentImposterMonthlyPackDto result = service.getCurrentMonthlyPack(learnerAuthUser());

        assertThat(result.exists()).isFalse();
        assertThat(result.yearMonth()).isEqualTo("2026-04");
        assertThat(result.currentWeekSlot()).isEqualTo((short) 2);
        assertThat(result.visibleConcepts()).isEmpty();
        assertThat(result.weeklyFeaturedSlots()).hasSize(4);
        assertThat(result.weeklyFeaturedSlots()).allMatch(slot -> !slot.revealed() && slot.conceptPublicId() == null && slot.title() == null);
    }

    @Test
    void weekOneRevealsOnlyFirstWeeklyFeaturedConcept() {
        SetupData data = setupPackData();
        when(imposterWeeklyFeaturedConceptService.currentWeekSlotInMonth()).thenReturn((short) 1);

        LearnerCurrentImposterMonthlyPackDto result = service.getCurrentMonthlyPack(learnerAuthUser());

        assertThat(result.exists()).isTrue();
        assertThat(result.visibleConcepts()).hasSize(17);

        UUID week1ConceptId = data.weeklyFeatures().get(0).getConcept().getPublicId();
        UUID week2ConceptId = data.weeklyFeatures().get(1).getConcept().getPublicId();
        UUID week3ConceptId = data.weeklyFeatures().get(2).getConcept().getPublicId();
        UUID week4ConceptId = data.weeklyFeatures().get(3).getConcept().getPublicId();

        assertThat(result.visibleConcepts()).anyMatch(concept -> concept.conceptPublicId().equals(week1ConceptId)
                && concept.weeklyFeatured() && concept.weeklyFeatureWeekSlot() == 1);
        assertThat(result.visibleConcepts()).noneMatch(concept -> concept.conceptPublicId().equals(week2ConceptId));
        assertThat(result.visibleConcepts()).noneMatch(concept -> concept.conceptPublicId().equals(week3ConceptId));
        assertThat(result.visibleConcepts()).noneMatch(concept -> concept.conceptPublicId().equals(week4ConceptId));

        assertThat(result.weeklyFeaturedSlots()).hasSize(4);
        assertThat(result.weeklyFeaturedSlots().get(0).revealed()).isTrue();
        assertThat(result.weeklyFeaturedSlots().get(0).conceptPublicId()).isEqualTo(week1ConceptId);
        assertThat(result.weeklyFeaturedSlots().get(1).revealed()).isFalse();
        assertThat(result.weeklyFeaturedSlots().get(1).conceptPublicId()).isNull();
        assertThat(result.weeklyFeaturedSlots().get(2).revealed()).isFalse();
        assertThat(result.weeklyFeaturedSlots().get(3).revealed()).isFalse();
    }

    @Test
    void weekThreeRevealsThreeWeeklyFeaturedConceptsAndHidesWeekFour() {
        SetupData data = setupPackData();
        when(imposterWeeklyFeaturedConceptService.currentWeekSlotInMonth()).thenReturn((short) 3);

        LearnerCurrentImposterMonthlyPackDto result = service.getCurrentMonthlyPack(learnerAuthUser());

        assertThat(result.visibleConcepts()).hasSize(19);

        UUID week1ConceptId = data.weeklyFeatures().get(0).getConcept().getPublicId();
        UUID week2ConceptId = data.weeklyFeatures().get(1).getConcept().getPublicId();
        UUID week3ConceptId = data.weeklyFeatures().get(2).getConcept().getPublicId();
        UUID week4ConceptId = data.weeklyFeatures().get(3).getConcept().getPublicId();

        assertThat(result.visibleConcepts()).anyMatch(concept -> concept.conceptPublicId().equals(week1ConceptId)
                && concept.weeklyFeatured() && concept.weeklyFeatureWeekSlot() == 1);
        assertThat(result.visibleConcepts()).anyMatch(concept -> concept.conceptPublicId().equals(week2ConceptId)
                && concept.weeklyFeatured() && concept.weeklyFeatureWeekSlot() == 2);
        assertThat(result.visibleConcepts()).anyMatch(concept -> concept.conceptPublicId().equals(week3ConceptId)
                && concept.weeklyFeatured() && concept.weeklyFeatureWeekSlot() == 3);
        assertThat(result.visibleConcepts()).noneMatch(concept -> concept.conceptPublicId().equals(week4ConceptId));

        assertThat(result.weeklyFeaturedSlots().get(0).revealed()).isTrue();
        assertThat(result.weeklyFeaturedSlots().get(1).revealed()).isTrue();
        assertThat(result.weeklyFeaturedSlots().get(2).revealed()).isTrue();
        assertThat(result.weeklyFeaturedSlots().get(3).revealed()).isFalse();
        assertThat(result.weeklyFeaturedSlots().get(3).conceptPublicId()).isNull();
    }

    private SetupData setupPackData() {
        ImposterMonthlyPack pack = new ImposterMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", 44L);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));

        List<Concept> concepts = java.util.stream.IntStream.rangeClosed(1, 20)
                .mapToObj(this::concept)
                .toList();

        List<ImposterMonthlyPackConcept> conceptRows = java.util.stream.IntStream.range(0, concepts.size())
                .mapToObj(index -> packConcept(pack, concepts.get(index), (short) (index + 1)))
                .toList();

        List<ImposterMonthlyPackWeeklyFeature> weeklyFeatures = List.of(
                weeklyFeature(pack, concepts.get(1), (short) 1),
                weeklyFeature(pack, concepts.get(5), (short) 2),
                weeklyFeature(pack, concepts.get(9), (short) 3),
                weeklyFeature(pack, concepts.get(13), (short) 4)
        );

        when(imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(44L)).thenReturn(conceptRows);
        when(imposterMonthlyPackWeeklyFeatureRepository.findByPack_IdOrderByWeekSlotAsc(44L)).thenReturn(weeklyFeatures);

        return new SetupData(weeklyFeatures);
    }

    private Concept concept(int number) {
        Concept concept = new Concept();
        ReflectionTestUtils.setField(concept, "publicId", UUID.randomUUID());
        concept.setTitle("concept-" + number);
        concept.setDescription("description-" + number);
        concept.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        return concept;
    }

    private ImposterMonthlyPackConcept packConcept(ImposterMonthlyPack pack, Concept concept, short slotIndex) {
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

    private SupabaseAuthUser learnerAuthUser() {
        UUID userId = UUID.randomUUID();
        Learner learner = new Learner();
        learner.setId(userId);
        learner.setPublicId(UUID.randomUUID());
        learner.setUsername("learner-" + userId.toString().substring(0, 8));
        learner.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        learner.setTotalPoints((short) 0);
        return new SupabaseAuthUser(userId, learner, null);
    }

    private record SetupData(List<ImposterMonthlyPackWeeklyFeature> weeklyFeatures) {
    }
}
