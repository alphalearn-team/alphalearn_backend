package com.example.demo.game.admin;

import com.example.demo.game.admin.dto.AdminGameMonthlyPackConceptDto;
import com.example.demo.game.admin.dto.AdminGameMonthlyPackDto;
import com.example.demo.game.admin.dto.UpsertAdminGameMonthlyPackRequest;
import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.game.monthly.GameMonthlyPack;
import com.example.demo.game.monthly.GameMonthlyPackConcept;
import com.example.demo.game.monthly.GameMonthlyPackWeeklyFeature;
import com.example.demo.game.monthly.repository.GameMonthlyPackConceptRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackWeeklyFeatureRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminGameMonthlyPackService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final GameMonthlyPackRepository imposterMonthlyPackRepository;
    private final GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final GameMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;
    private final ConceptRepository conceptRepository;
    private final Clock clock;

    public AdminGameMonthlyPackService(
            GameMonthlyPackRepository imposterMonthlyPackRepository,
            GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            GameMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository,
            ConceptRepository conceptRepository,
            Clock clock
    ) {
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.imposterMonthlyPackWeeklyFeatureRepository = imposterMonthlyPackWeeklyFeatureRepository;
        this.conceptRepository = conceptRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminGameMonthlyPackDto getMonthlyPack(String yearMonth) {
        String normalizedYearMonth = normalizeYearMonth(yearMonth);

        return imposterMonthlyPackRepository.findByYearMonth(normalizedYearMonth)
                .map(pack -> toDto(pack, normalizedYearMonth))
                .orElseGet(() -> emptyDto(normalizedYearMonth));
    }

    @Transactional(readOnly = true)
    public AdminGameMonthlyPackDto getCurrentMonthlyPack() {
        String currentYearMonth = YearMonth.now(clock).format(YEAR_MONTH_FORMATTER);
        return getMonthlyPack(currentYearMonth);
    }

    @Transactional
    public AdminGameMonthlyPackDto upsertMonthlyPack(String yearMonth, UpsertAdminGameMonthlyPackRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        String normalizedYearMonth = normalizeYearMonth(yearMonth);
        List<UUID> conceptPublicIds = validateExactUniqueIds(request.conceptPublicIds(), 20, "conceptPublicIds");
        List<UUID> weeklyFeaturedConceptPublicIds = validateExactUniqueIds(
                request.weeklyFeaturedConceptPublicIds(),
                4,
                "weeklyFeaturedConceptPublicIds"
        );

        Set<UUID> conceptIdSet = new LinkedHashSet<>(conceptPublicIds);
        if (!conceptIdSet.containsAll(weeklyFeaturedConceptPublicIds)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "weeklyFeaturedConceptPublicIds must be selected from conceptPublicIds"
            );
        }

        List<Concept> resolvedConcepts = conceptRepository.findAllByPublicIdIn(conceptPublicIds);
        if (resolvedConcepts.size() != conceptPublicIds.size()) {
            Set<UUID> found = resolvedConcepts.stream()
                    .map(Concept::getPublicId)
                    .collect(java.util.stream.Collectors.toSet());
            List<UUID> missing = conceptPublicIds.stream()
                    .filter(publicId -> !found.contains(publicId))
                    .toList();
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept not found: " + missing);
        }

        Map<UUID, Concept> conceptByPublicId = resolvedConcepts.stream()
                .collect(java.util.stream.Collectors.toMap(Concept::getPublicId, concept -> concept, (left, right) -> left, LinkedHashMap::new));

        List<Concept> orderedConcepts = conceptPublicIds.stream()
                .map(conceptByPublicId::get)
                .filter(Objects::nonNull)
                .toList();

        GameMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(normalizedYearMonth)
                .orElseGet(() -> createMonthlyPack(normalizedYearMonth));

        imposterMonthlyPackWeeklyFeatureRepository.deleteByPack_Id(pack.getId());
        imposterMonthlyPackWeeklyFeatureRepository.flush();

        imposterMonthlyPackConceptRepository.deleteByPack_Id(pack.getId());
        imposterMonthlyPackConceptRepository.flush();

        // Force old rows to clear before re-inserting same slot_index values (1..20).
        imposterMonthlyPackRepository.flush();

        List<GameMonthlyPackConcept> conceptRows = java.util.stream.IntStream.range(0, orderedConcepts.size())
                .mapToObj(index -> {
                    GameMonthlyPackConcept row = new GameMonthlyPackConcept();
                    row.setPack(pack);
                    row.setConcept(orderedConcepts.get(index));
                    row.setSlotIndex((short) (index + 1));
                    return row;
                })
                .toList();
        imposterMonthlyPackConceptRepository.saveAll(conceptRows);

        List<GameMonthlyPackWeeklyFeature> weeklyFeatureRows = java.util.stream.IntStream.range(0, weeklyFeaturedConceptPublicIds.size())
                .mapToObj(index -> {
                    GameMonthlyPackWeeklyFeature row = new GameMonthlyPackWeeklyFeature();
                    row.setPack(pack);
                    row.setConcept(conceptByPublicId.get(weeklyFeaturedConceptPublicIds.get(index)));
                    row.setWeekSlot((short) (index + 1));
                    return row;
                })
                .toList();
        imposterMonthlyPackWeeklyFeatureRepository.saveAll(weeklyFeatureRows);

        List<AdminGameMonthlyPackConceptDto> conceptDtos = conceptRows.stream()
                .map(entry -> new AdminGameMonthlyPackConceptDto(
                        entry.getSlotIndex(),
                        entry.getConcept().getPublicId(),
                        entry.getConcept().getTitle()
                ))
                .toList();

        return new AdminGameMonthlyPackDto(
                normalizedYearMonth,
                true,
                conceptDtos,
                weeklyFeaturedConceptPublicIds
        );
    }

    private GameMonthlyPack createMonthlyPack(String yearMonth) {
        GameMonthlyPack pack = new GameMonthlyPack();
        pack.setYearMonth(yearMonth);
        pack.setCreatedAt(OffsetDateTime.now(clock));
        return imposterMonthlyPackRepository.save(pack);
    }

    private AdminGameMonthlyPackDto toDto(GameMonthlyPack pack, String yearMonth) {
        List<AdminGameMonthlyPackConceptDto> concepts = imposterMonthlyPackConceptRepository
                .findByPack_IdOrderBySlotIndexAsc(pack.getId())
                .stream()
                .map(entry -> new AdminGameMonthlyPackConceptDto(
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

        return new AdminGameMonthlyPackDto(
                yearMonth,
                true,
                concepts,
                weeklyFeaturedConceptPublicIds
        );
    }

    private AdminGameMonthlyPackDto emptyDto(String yearMonth) {
        return new AdminGameMonthlyPackDto(
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

    private List<UUID> validateExactUniqueIds(List<UUID> ids, int expectedSize, String fieldName) {
        if (ids == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        if (ids.size() != expectedSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must contain exactly " + expectedSize + " items");
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " cannot contain null values");
        }
        if (new LinkedHashSet<>(ids).size() != expectedSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must contain unique values");
        }

        return List.copyOf(ids);
    }
}
