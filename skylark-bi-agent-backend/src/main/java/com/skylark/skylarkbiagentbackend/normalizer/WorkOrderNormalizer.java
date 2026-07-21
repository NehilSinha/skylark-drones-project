package com.skylark.skylarkbiagentbackend.normalizer;

import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayItemDto;
import com.skylark.skylarkbiagentbackend.config.WorkOrderColumnMapping;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes raw Work Order Tracker items into {@link NormalizedWorkOrder}. Handles
 * the data-quality issues found in Phase 1 profiling: mixed-case status values
 * (e.g. {@code BIlled}/{@code Billed}), blank statuses, blank sector, and
 * quantity fields mixing a number with a unit (e.g. {@code "5360 HA"} — handled by
 * {@link NumericParser#parseAmount}, which already extracts the numeric portion
 * from mixed text).
 */
@Component
public class WorkOrderNormalizer {

    private static final Set<String> STRAY_HEADER_VALUES = Set.of(
            "deal name masked", "customer name code", "execution status");

    /** Lowercased raw value -> canonical display value, for known casing bugs in the source data. */
    private static final Map<String, String> STATUS_ALIASES = Map.of(
            "billed", "Billed"
    );

    public boolean isValidRecord(MondayItemDto item) {
        String name = item.name() == null ? "" : item.name().trim().toLowerCase(Locale.ROOT);
        return !name.isBlank() && !STRAY_HEADER_VALUES.contains(name);
    }

    public NormalizedWorkOrder normalize(MondayItemDto item, WorkOrderColumnMapping mapping) {
        Map<String, String> columns = MondayColumnTextExtractor.columnTextById(item);

        return new NormalizedWorkOrder(
                item.id(),
                TextNormalizationUtils.normalize(item.name()),
                TextNormalizationUtils.normalizeToNull(columns.get(mapping.customerCode())),
                TextNormalizationUtils.normalizeToNull(columns.get(mapping.ownerCode())),
                normalizeSector(columns.get(mapping.sector())),
                canonicalizeStatus(columns.get(mapping.executionStatus())),
                DateParser.parse(columns.get(mapping.probableEndDate())),
                DateParser.parse(columns.get(mapping.dataDeliveryDate())),
                canonicalizeStatus(columns.get(mapping.billingStatus())),
                canonicalizeStatus(columns.get(mapping.invoiceStatus())),
                NumericParser.parseAmount(columns.get(mapping.amountToBeBilled())).orElse(null),
                NumericParser.parseAmount(columns.get(mapping.collectedAmount())).orElse(null),
                NumericParser.parseAmount(columns.get(mapping.amountReceivable())).orElse(null),
                NumericParser.parseAmount(columns.get(mapping.quantityAsPerPo())).orElse(null),
                NumericParser.parseAmount(columns.get(mapping.quantityBilled())).orElse(null)
        );
    }

    private String canonicalizeStatus(String raw) {
        String normalized = TextNormalizationUtils.normalizeToNull(raw);
        if (normalized == null) {
            return "Not Set";
        }
        String alias = STATUS_ALIASES.get(normalized.toLowerCase(Locale.ROOT));
        return alias != null ? alias : normalized;
    }

    private String normalizeSector(String raw) {
        String normalized = TextNormalizationUtils.normalizeToNull(raw);
        return normalized == null ? "Unspecified" : normalized;
    }
}
