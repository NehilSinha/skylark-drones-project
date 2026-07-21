package com.skylark.skylarkbiagentbackend.normalizer;

import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayItemDto;
import com.skylark.skylarkbiagentbackend.config.DealColumnMapping;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes raw Deal Tracker items into {@link NormalizedDeal}. Handles exactly
 * the data-quality issues found in the real source data during Phase 1 profiling:
 * blank revenue/probability/owner/sector, mixed capitalization, extra whitespace,
 * malformed numerics, and stray re-embedded header rows.
 */
@Component
public class DealNormalizer {

    /** Literal values seen in the raw export where a header row got re-embedded as data. */
    private static final Set<String> STRAY_HEADER_VALUES = Set.of(
            "deal name", "deal status", "sector/service", "deal stage");

    public boolean isValidRecord(MondayItemDto item) {
        String name = item.name() == null ? "" : item.name().trim().toLowerCase(Locale.ROOT);
        return !name.isBlank() && !STRAY_HEADER_VALUES.contains(name);
    }

    public NormalizedDeal normalize(MondayItemDto item, DealColumnMapping mapping) {
        Map<String, String> columns = MondayColumnTextExtractor.columnTextById(item);

        return new NormalizedDeal(
                item.id(),
                TextNormalizationUtils.normalize(item.name()),
                TextNormalizationUtils.normalizeToNull(columns.get(mapping.ownerCode())),
                TextNormalizationUtils.normalizeToNull(columns.get(mapping.clientCode())),
                parseStatus(columns.get(mapping.dealStatus())),
                DateParser.parse(columns.get(mapping.closeDate())),
                parseProbability(columns.get(mapping.closureProbability())),
                NumericParser.parseAmount(columns.get(mapping.dealValue())).orElse(null),
                DateParser.parse(columns.get(mapping.tentativeCloseDate())),
                TextNormalizationUtils.normalizeToNull(columns.get(mapping.dealStage())),
                TextNormalizationUtils.normalizeToNull(columns.get(mapping.productDeal())),
                normalizeSector(columns.get(mapping.sector())),
                DateParser.parse(columns.get(mapping.createdDate()))
        );
    }

    private DealStatus parseStatus(String raw) {
        String normalized = TextNormalizationUtils.normalizeToNull(raw);
        if (normalized == null) {
            return DealStatus.UNKNOWN;
        }
        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "won" -> DealStatus.WON;
            case "dead" -> DealStatus.DEAD;
            case "open" -> DealStatus.OPEN;
            case "on hold" -> DealStatus.ON_HOLD;
            default -> DealStatus.UNKNOWN;
        };
    }

    private ClosureProbability parseProbability(String raw) {
        String normalized = TextNormalizationUtils.normalizeToNull(raw);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "high" -> ClosureProbability.HIGH;
            case "medium" -> ClosureProbability.MEDIUM;
            case "low" -> ClosureProbability.LOW;
            default -> null;
        };
    }

    private String normalizeSector(String raw) {
        String normalized = TextNormalizationUtils.normalizeToNull(raw);
        return normalized == null ? "Unspecified" : normalized;
    }
}
