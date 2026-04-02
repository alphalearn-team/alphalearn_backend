package com.example.demo.me.imposter;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackConcept;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackWeeklyFeature;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackWeeklyFeatureRepository;
import com.example.demo.me.imposter.dto.LearnerCurrentImposterMonthlyPackDto;
import com.example.demo.me.imposter.dto.LearnerImposterMonthlyPackVisibleConceptDto;
import com.example.demo.me.imposter.dto.LearnerImposterMonthlyPackWeeklyFeaturedSlotDto;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
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
    private static final ZoneId WEEK_SLOT_ZONE_ID = ZoneId.of("Asia/Singapore");

    private final ImposterMonthlyPackRepository imposterMonthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final ImposterMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;
    private final Clock clock;

    public LearnerImposterMonthlyPackService(
            ImposterMonthlyPackRepository imposterMonthlyPackRepository,
            ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            ImposterMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository,
            Clock clock
    ) {
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.imposterMonthlyPackWeeklyFeatureRepository = imposterMonthlyPackWeeklyFeatureRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LearnerCurrentImposterMonthlyPackDto getCurrentMonthlyPack(SupabaseAuthUser user) {
        requireLearner(user);

        String yearMonth = YearMonth.now(clock).format(YEAR_MONTH_FORMATTER);
        short currentWeekSlot = currentWeekSlotInMonth();

        ImposterMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(yearMonth).orElse(null);
        if (pack == null) {
            return new LearnerCurrentImposterMonthlyPackDto(
                    yearMonth,
                    false,
                    currentWeekSlot,
                    List.of(),
                    List.of()
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
                    return new LearnerImposterMonthlyPackWeeklyFeaturedSlotDto(
                            weekSlot,
                            weekSlot <= currentWeekSlot,
                            row == null ? null : row.getConcept().getPublicId(),
                            row == null ? null : row.getConcept().getTitle()
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

    short currentWeekSlotInMonth() {
        java.time.LocalDate singaporeDate = java.time.LocalDate.now(clock.withZone(WEEK_SLOT_ZONE_ID));
        int dayOfMonth = singaporeDate.getDayOfMonth();
        int slot = ((dayOfMonth - 1) / 7) + 1;
        return (short) Math.min(slot, 4);
    }
}
