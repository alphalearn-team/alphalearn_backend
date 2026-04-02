package com.example.demo.admin.imposter;

import com.example.demo.admin.imposter.dto.AdminImposterMonthlyPackConceptDto;
import com.example.demo.admin.imposter.dto.AdminImposterMonthlyPackDto;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackWeeklyFeatureRepository;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminImposterMonthlyPackService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ImposterMonthlyPackRepository imposterMonthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final ImposterMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;
    private final Clock clock;

    public AdminImposterMonthlyPackService(
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
    public AdminImposterMonthlyPackDto getMonthlyPack(String yearMonth) {
        String normalizedYearMonth = normalizeYearMonth(yearMonth);

        return imposterMonthlyPackRepository.findByYearMonth(normalizedYearMonth)
                .map(pack -> toDto(pack, normalizedYearMonth))
                .orElseGet(() -> emptyDto(normalizedYearMonth));
    }

    @Transactional(readOnly = true)
    public AdminImposterMonthlyPackDto getCurrentMonthlyPack() {
        String currentYearMonth = YearMonth.now(clock).format(YEAR_MONTH_FORMATTER);
        return getMonthlyPack(currentYearMonth);
    }

    private AdminImposterMonthlyPackDto toDto(ImposterMonthlyPack pack, String yearMonth) {
        List<AdminImposterMonthlyPackConceptDto> concepts = imposterMonthlyPackConceptRepository
                .findByPack_IdOrderBySlotIndexAsc(pack.getId())
                .stream()
                .map(entry -> new AdminImposterMonthlyPackConceptDto(
                        entry.getSlotIndex(),
                        entry.getConcept().getPublicId(),
                        entry.getConcept().getTitle()
                ))
                .toList();

        List<UUID> weeklyFeaturedConceptPublicIds = imposterMonthlyPackWeeklyFeatureRepository
                .findByPack_IdOrderByWeekSlotAsc(pack.getId())
                .stream()
                .map(feature -> feature.getConcept().getPublicId())
                .toList();

        return new AdminImposterMonthlyPackDto(
                yearMonth,
                true,
                concepts,
                weeklyFeaturedConceptPublicIds
        );
    }

    private AdminImposterMonthlyPackDto emptyDto(String yearMonth) {
        return new AdminImposterMonthlyPackDto(
                yearMonth,
                false,
                List.of(),
                List.of()
        );
    }

    private String normalizeYearMonth(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "yearMonth is required");
        }

        try {
            return YearMonth.parse(value, YEAR_MONTH_FORMATTER).format(YEAR_MONTH_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "yearMonth must use yyyy-MM format");
        }
    }
}
