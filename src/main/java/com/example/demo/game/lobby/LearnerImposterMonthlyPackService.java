package com.example.demo.game.lobby;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.ImposterWeeklyFeaturedConceptService;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackConcept;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackWeeklyFeature;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackWeeklyFeatureRepository;
import com.example.demo.game.lobby.dto.LearnerCurrentImposterMonthlyPackDto;
import com.example.demo.game.lobby.dto.LearnerImposterMonthlyPackVisibleConceptDto;
import com.example.demo.game.lobby.dto.LearnerImposterMonthlyPackWeeklyFeaturedSlotDto;
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
public class LearnerImposterMonthlyPackService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ImposterMonthlyPackRepository imposterMonthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final ImposterMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;
    private final ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;
    private final Clock clock;

    public LearnerImposterMonthlyPackService(
            ImposterMonthlyPackRepository imposterMonthlyPackRepository,
            ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            ImposterMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository,
            ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService,
            Clock clock
    ) {
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.imposterMonthlyPackWeeklyFeatureRepository = imposterMonthlyPackWeeklyFeatureRepository;
        this.imposterWeeklyFeaturedConceptService = imposterWeeklyFeaturedConceptService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LearnerCurrentImposterMonthlyPackDto getCurrentMonthlyPack(SupabaseAuthUser user) {
        requireLearner(user);

        String yearMonth = YearMonth.now(clock).format(YEAR_MONTH_FORMATTER);
        short currentWeekSlot = imposterWeeklyFeaturedConceptService.currentWeekSlotInMonth();

        ImposterMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(yearMonth).orElse(null);
        if (pack == null) {
            return new LearnerCurrentImposterMonthlyPackDto(
                    yearMonth,
                    false,
                    currentWeekSlot,
                    List.of(),
                    hiddenWeeklyFeaturedSlots()
            );
        }

        List<ImposterMonthlyPackConcept> conceptRows = imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(pack.getId());
        List<ImposterMonthlyPackWeeklyFeature> weeklyFeatureRows = imposterMonthlyPackWeeklyFeatureRepository.findByPack_IdOrderByWeekSlotAsc(pack.getId());

        Map<UUID, Short> weeklyFeatureByConceptPublicId = weeklyFeatureRows.stream()
                .collect(Collectors.toMap(
                        row -> row.getConcept().getPublicId(),
                        ImposterMonthlyPackWeeklyFeature::getWeekSlot,
                        (left, right) -> left
                ));

        List<LearnerImposterMonthlyPackVisibleConceptDto> visibleConcepts = conceptRows.stream()
                .filter(row -> {
                    Short weekSlot = weeklyFeatureByConceptPublicId.get(row.getConcept().getPublicId());
                    return weekSlot == null || weekSlot <= currentWeekSlot;
                })
                .map(row -> {
                    Short weekSlot = weeklyFeatureByConceptPublicId.get(row.getConcept().getPublicId());
                    return new LearnerImposterMonthlyPackVisibleConceptDto(
                            row.getSlotIndex(),
                            row.getConcept().getPublicId(),
                            row.getConcept().getTitle(),
                            weekSlot != null,
                            weekSlot
                    );
                })
                .toList();

        Map<Short, ImposterMonthlyPackWeeklyFeature> weeklyFeatureBySlot = weeklyFeatureRows.stream()
                .collect(Collectors.toMap(
                        ImposterMonthlyPackWeeklyFeature::getWeekSlot,
                        Function.identity(),
                        (left, right) -> left
                ));

        List<LearnerImposterMonthlyPackWeeklyFeaturedSlotDto> weeklyFeaturedSlots = IntStream.rangeClosed(1, 4)
                .mapToObj(slotValue -> {
                    short weekSlot = (short) slotValue;
                    ImposterMonthlyPackWeeklyFeature row = weeklyFeatureBySlot.get(weekSlot);
                    boolean revealed = weekSlot <= currentWeekSlot;
                    return new LearnerImposterMonthlyPackWeeklyFeaturedSlotDto(
                            weekSlot,
                            revealed,
                            !revealed || row == null ? null : row.getConcept().getPublicId(),
                            !revealed || row == null ? null : row.getConcept().getTitle()
                    );
                })
                .toList();

        return new LearnerCurrentImposterMonthlyPackDto(
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

    private List<LearnerImposterMonthlyPackWeeklyFeaturedSlotDto> hiddenWeeklyFeaturedSlots() {
        return IntStream.rangeClosed(1, 4)
                .mapToObj(slot -> new LearnerImposterMonthlyPackWeeklyFeaturedSlotDto(
                        (short) slot,
                        false,
                        null,
                        null
                ))
                .toList();
    }
}
