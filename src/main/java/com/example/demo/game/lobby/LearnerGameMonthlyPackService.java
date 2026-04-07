package com.example.demo.game.lobby;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.GameWeeklyFeaturedConceptService;
import com.example.demo.game.monthly.GameMonthlyPack;
import com.example.demo.game.monthly.GameMonthlyPackConcept;
import com.example.demo.game.monthly.GameMonthlyPackWeeklyFeature;
import com.example.demo.game.monthly.repository.GameMonthlyPackConceptRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackWeeklyFeatureRepository;
import com.example.demo.game.lobby.dto.LearnerCurrentGameMonthlyPackDto;
import com.example.demo.game.lobby.dto.LearnerGameMonthlyPackVisibleConceptDto;
import com.example.demo.game.lobby.dto.LearnerGameMonthlyPackWeeklyFeaturedSlotDto;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearnerGameMonthlyPackService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final GameMonthlyPackRepository imposterMonthlyPackRepository;
    private final GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final GameMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;
    private final GameWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;
    private final Clock clock;

    public LearnerGameMonthlyPackService(
            GameMonthlyPackRepository imposterMonthlyPackRepository,
            GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            GameMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository,
            GameWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService,
            Clock clock
    ) {
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.imposterMonthlyPackWeeklyFeatureRepository = imposterMonthlyPackWeeklyFeatureRepository;
        this.imposterWeeklyFeaturedConceptService = imposterWeeklyFeaturedConceptService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LearnerCurrentGameMonthlyPackDto getCurrentMonthlyPack(SupabaseAuthUser user) {
        requireLearner(user);

        String yearMonth = YearMonth.now(clock).format(YEAR_MONTH_FORMATTER);
        short currentWeekSlot = imposterWeeklyFeaturedConceptService.currentWeekSlotInMonth();

        GameMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(yearMonth).orElse(null);
        if (pack == null) {
            return new LearnerCurrentGameMonthlyPackDto(
                    yearMonth,
                    false,
                    currentWeekSlot,
                    List.of(),
                    hiddenWeeklyFeaturedSlots()
            );
        }

        List<GameMonthlyPackConcept> conceptRows = imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(pack.getId());
        List<GameMonthlyPackWeeklyFeature> weeklyFeatureRows = imposterMonthlyPackWeeklyFeatureRepository.findByPack_IdOrderByWeekSlotAsc(pack.getId());

        Map<UUID, Short> weeklyFeatureByConceptPublicId = weeklyFeatureRows.stream()
                .collect(Collectors.toMap(
                        row -> row.getConcept().getPublicId(),
                        GameMonthlyPackWeeklyFeature::getWeekSlot,
                        (left, right) -> left
                ));

        List<LearnerGameMonthlyPackVisibleConceptDto> visibleConcepts = conceptRows.stream()
                .filter(row -> {
                    Short weekSlot = weeklyFeatureByConceptPublicId.get(row.getConcept().getPublicId());
                    return weekSlot == null || weekSlot <= currentWeekSlot;
                })
                .map(row -> {
                    Short weekSlot = weeklyFeatureByConceptPublicId.get(row.getConcept().getPublicId());
                    return new LearnerGameMonthlyPackVisibleConceptDto(
                            row.getSlotIndex(),
                            row.getConcept().getPublicId(),
                            row.getConcept().getTitle(),
                            weekSlot != null,
                            weekSlot
                    );
                })
                .toList();

        Map<Short, GameMonthlyPackWeeklyFeature> weeklyFeatureBySlot = weeklyFeatureRows.stream()
                .collect(Collectors.toMap(
                        GameMonthlyPackWeeklyFeature::getWeekSlot,
                        Function.identity(),
                        (left, right) -> left
                ));

        List<LearnerGameMonthlyPackWeeklyFeaturedSlotDto> weeklyFeaturedSlots = IntStream.rangeClosed(1, 4)
                .mapToObj(slotValue -> {
                    short weekSlot = (short) slotValue;
                    GameMonthlyPackWeeklyFeature row = weeklyFeatureBySlot.get(weekSlot);
                    boolean revealed = weekSlot <= currentWeekSlot;
                    return new LearnerGameMonthlyPackWeeklyFeaturedSlotDto(
                            weekSlot,
                            revealed,
                            !revealed || row == null ? null : row.getConcept().getPublicId(),
                            !revealed || row == null ? null : row.getConcept().getTitle()
                    );
                })
                .toList();

        return new LearnerCurrentGameMonthlyPackDto(
                yearMonth,
                true,
                currentWeekSlot,
                visibleConcepts,
                weeklyFeaturedSlots
        );
    }

    private void requireLearner(SupabaseAuthUser user) {
        if (user == null || user.userId() == null || user.learner() == null || !user.isLearner()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
    }

    private List<LearnerGameMonthlyPackWeeklyFeaturedSlotDto> hiddenWeeklyFeaturedSlots() {
        return IntStream.rangeClosed(1, 4)
                .mapToObj(slot -> new LearnerGameMonthlyPackWeeklyFeaturedSlotDto(
                        (short) slot,
                        false,
                        null,
                        null
                ))
                .toList();
    }
}
