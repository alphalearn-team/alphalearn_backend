package com.example.demo.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.demo.concept.Concept;
import com.example.demo.game.monthly.GameMonthlyPack;
import com.example.demo.game.monthly.GameMonthlyPackWeeklyFeature;
import com.example.demo.game.monthly.repository.GameMonthlyPackRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackWeeklyFeatureRepository;
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
class GameWeeklyFeaturedConceptServiceTest {

    @Mock
    private GameMonthlyPackRepository imposterMonthlyPackRepository;

    @Mock
    private GameMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;

    @Test
    void mapsWeekSlotUsingSingaporeTimezoneWithClampToFourthWeek() {
        GameWeeklyFeaturedConceptService firstWeekService = serviceAt("2026-04-01T01:00:00Z");
        GameWeeklyFeaturedConceptService secondWeekService = serviceAt("2026-04-08T01:00:00Z");
        GameWeeklyFeaturedConceptService fourthWeekService = serviceAt("2026-04-29T01:00:00Z");

        assertThat(firstWeekService.currentWeekSlotInMonth()).isEqualTo((short) 1);
        assertThat(secondWeekService.currentWeekSlotInMonth()).isEqualTo((short) 2);
        assertThat(fourthWeekService.currentWeekSlotInMonth()).isEqualTo((short) 4);
    }

    @Test
    void resolvesFeaturedConceptForCurrentWeekSlot() {
        GameWeeklyFeaturedConceptService service = serviceAt("2026-04-15T01:00:00Z");

        GameMonthlyPack pack = new GameMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", 50L);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));

        Concept concept = new Concept();
        ReflectionTestUtils.setField(concept, "publicId", UUID.randomUUID());
        concept.setTitle("depth-first search");
        concept.setDescription("graph traversal");
        concept.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));

        GameMonthlyPackWeeklyFeature feature = new GameMonthlyPackWeeklyFeature();
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

    private GameWeeklyFeaturedConceptService serviceAt(String instant) {
        Clock fixedClock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new GameWeeklyFeaturedConceptService(
                imposterMonthlyPackRepository,
                imposterMonthlyPackWeeklyFeatureRepository,
                fixedClock
        );
    }
}
