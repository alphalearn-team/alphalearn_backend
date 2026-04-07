package com.example.demo.game.admin;

import com.example.demo.game.admin.dto.AdminImposterMonthlyPackConceptDto;
import com.example.demo.game.admin.dto.AdminImposterMonthlyPackDto;
import com.example.demo.game.admin.dto.UpsertAdminImposterMonthlyPackRequest;
import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackConcept;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackWeeklyFeature;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackWeeklyFeatureRepository;
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
public class AdminImposterMonthlyPackService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ImposterMonthlyPackRepository imposterMonthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final ImposterMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;
    private final ConceptRepository conceptRepository;
    private final Clock clock;

    public AdminImposterMonthlyPackService(
            ImposterMonthlyPackRepository imposterMonthlyPackRepository,
            ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            ImposterMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository,
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

    @Transactional
    public AdminImposterMonthlyPackDto upsertMonthlyPack(String yearMonth, UpsertAdminImposterMonthlyPackRequest request) {
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

        ImposterMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(normalizedYearMonth)
                .orElseGet(() -> createMonthlyPack(normalizedYearMonth));

        imposterMonthlyPackWeeklyFeatureRepository.deleteByPack_Id(pack.getId());
        imposterMonthlyPackWeeklyFeatureRepository.flush();

        imposterMonthlyPackConceptRepository.deleteByPack_Id(pack.getId());
        imposterMonthlyPackConceptRepository.flush();

        // Force old rows to clear before re-inserting same slot_index values (1..20).
        imposterMonthlyPackRepository.flush();

        List<ImposterMonthlyPackConcept> conceptRows = java.util.stream.IntStream.range(0, orderedConcepts.size())
                .mapToObj(index -> {
                    ImposterMonthlyPackConcept row = new ImposterMonthlyPackConcept();
                    row.setPack(pack);
                    row.setConcept(orderedConcepts.get(index));
                    row.setSlotIndex((short) (index + 1));
                    return row;
                })
                .toList();
        imposterMonthlyPackConceptRepository.saveAll(conceptRows);

        List<ImposterMonthlyPackWeeklyFeature> weeklyFeatureRows = java.util.stream.IntStream.range(0, weeklyFeaturedConceptPublicIds.size())
                .mapToObj(index -> {
                    ImposterMonthlyPackWeeklyFeature row = new ImposterMonthlyPackWeeklyFeature();
                    row.setPack(pack);
                    row.setConcept(conceptByPublicId.get(weeklyFeaturedConceptPublicIds.get(index)));
                    row.setWeekSlot((short) (index + 1));
                    return row;
                })
                .toList();
        imposterMonthlyPackWeeklyFeatureRepository.saveAll(weeklyFeatureRows);

        List<AdminImposterMonthlyPackConceptDto> conceptDtos = conceptRows.stream()
                .map(entry -> new AdminImposterMonthlyPackConceptDto(
                        entry.getSlotIndex(),
                        entry.getConcept().getPublicId(),
                        entry.getConcept().getTitle()
                ))
                .toList();

        return new AdminImposterMonthlyPackDto(
                normalizedYearMonth,
                true,
                conceptDtos,
                weeklyFeaturedConceptPublicIds
        );
    }

    private ImposterMonthlyPack createMonthlyPack(String yearMonth) {
        ImposterMonthlyPack pack = new ImposterMonthlyPack();
        pack.setYearMonth(yearMonth);
        pack.setCreatedAt(OffsetDateTime.now(clock));
        return imposterMonthlyPackRepository.save(pack);
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
