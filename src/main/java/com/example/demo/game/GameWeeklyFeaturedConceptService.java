package com.example.demo.game;

import com.example.demo.concept.Concept;
import com.example.demo.game.monthly.GameMonthlyPack;
import com.example.demo.game.monthly.repository.GameMonthlyPackRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackWeeklyFeatureRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GameWeeklyFeaturedConceptService {

    private static final ZoneId WEEK_SLOT_ZONE_ID = ZoneId.of("Asia/Singapore");

    private final GameMonthlyPackRepository imposterMonthlyPackRepository;
    private final GameMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;
    private final Clock clock;

    public GameWeeklyFeaturedConceptService(
            GameMonthlyPackRepository imposterMonthlyPackRepository,
            GameMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository,
            Clock clock
    ) {
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackWeeklyFeatureRepository = imposterMonthlyPackWeeklyFeatureRepository;
        this.clock = clock;
    }

    public Optional<Concept> resolveCurrentWeeklyFeaturedConcept(String yearMonth) {
        return imposterMonthlyPackRepository.findByYearMonth(yearMonth)
                .flatMap(pack -> resolveFeatureByWeekSlot(pack, currentWeekSlotInMonth()));
    }

    public short currentWeekSlotInMonth() {
        LocalDate singaporeDate = LocalDate.now(clock.withZone(WEEK_SLOT_ZONE_ID));
        int dayOfMonth = singaporeDate.getDayOfMonth();
        int slot = ((dayOfMonth - 1) / 7) + 1;
        return (short) Math.min(slot, 4);
    }

    private Optional<Concept> resolveFeatureByWeekSlot(GameMonthlyPack pack, short weekSlot) {
        return imposterMonthlyPackWeeklyFeatureRepository.findByPack_IdAndWeekSlot(pack.getId(), weekSlot)
                .map(feature -> feature.getConcept());
    }
}
