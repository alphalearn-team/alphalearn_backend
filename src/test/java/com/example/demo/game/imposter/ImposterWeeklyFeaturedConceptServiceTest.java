package com.example.demo.game.imposter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.demo.concept.Concept;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackWeeklyFeature;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackWeeklyFeatureRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ImposterWeeklyFeaturedConceptServiceTest {

    @Mock
    private ImposterMonthlyPackRepository imposterMonthlyPackRepository;

    @Mock
    private ImposterMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;

    @Test
    void mapsWeekSlotUsingSingaporeTimezoneWithClampToFourthWeek() {
        ImposterWeeklyFeaturedConceptService firstWeekService = serviceAt("2026-04-01T01:00:00Z");
        ImposterWeeklyFeaturedConceptService secondWeekService = serviceAt("2026-04-08T01:00:00Z");
        ImposterWeeklyFeaturedConceptService fourthWeekService = serviceAt("2026-04-29T01:00:00Z");

        assertThat(firstWeekService.currentWeekSlotInMonth()).isEqualTo((short) 1);
        assertThat(secondWeekService.currentWeekSlotInMonth()).isEqualTo((short) 2);
        assertThat(fourthWeekService.currentWeekSlotInMonth()).isEqualTo((short) 4);
    }

    @Test
    void resolvesFeaturedConceptForCurrentWeekSlot() {
        ImposterWeeklyFeaturedConceptService service = serviceAt("2026-04-15T01:00:00Z");

        ImposterMonthlyPack pack = new ImposterMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", 50L);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));

        Concept concept = new Concept();
        ReflectionTestUtils.setField(concept, "publicId", UUID.randomUUID());
        concept.setTitle("depth-first search");
        concept.setDescription("graph traversal");
        concept.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));

        ImposterMonthlyPackWeeklyFeature feature = new ImposterMonthlyPackWeeklyFeature();
        feature.setPack(pack);
        feature.setConcept(concept);
        feature.setWeekSlot((short) 3);

        when(imposterMonthlyPackWeeklyFeatureRepository.findByPack_IdAndWeekSlot(50L, (short) 3))
                .thenReturn(Optional.of(feature));

        assertThat(service.resolveCurrentWeeklyFeaturedConcept("2026-04"))
                .isPresent()
                .get()
                .extracting(Concept::getPublicId, Concept::getTitle)
                .containsExactly(concept.getPublicId(), "depth-first search");
    }

    private ImposterWeeklyFeaturedConceptService serviceAt(String instant) {
        Clock fixedClock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new ImposterWeeklyFeaturedConceptService(
                imposterMonthlyPackRepository,
                imposterMonthlyPackWeeklyFeatureRepository,
                fixedClock
        );
    }
}
