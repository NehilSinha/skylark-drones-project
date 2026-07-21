package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedWorkOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Collections and receivables in one snapshot — receivables is mathematically
 * derived from the same financial columns as collections, so this is one tool over
 * one service, not two (PHASE-4-DESIGN.md T11). "Collected" comes from
 * {@code collectedAmount}, never the raw "Collection status" text column (which
 * Phase 1 found contains month names in the real data, not a status enum).
 */
@Component
public class CollectionsAnalyticsService {

    private final WorkOrderQueryService workOrderQueryService;
    private final Clock clock;

    public CollectionsAnalyticsService(WorkOrderQueryService workOrderQueryService, Clock clock) {
        this.workOrderQueryService = workOrderQueryService;
        this.clock = clock;
    }

    public CollectionsAnalyticsResponse snapshot(EntityFilter filter) {
        List<NormalizedWorkOrder> workOrders = workOrderQueryService.queryAll(filter);
        List<DataQualityWarning> warnings = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);

        List<NormalizedWorkOrder> withCollected = workOrders.stream()
                .filter(wo -> wo.collectedAmount() != null)
                .toList();
        BigDecimal totalCollected = sum(withCollected, NormalizedWorkOrder::collectedAmount);

        BigDecimal totalReceivable = BigDecimal.ZERO;
        long excludedFromReceivable = 0;
        for (NormalizedWorkOrder wo : workOrders) {
            if (wo.amountReceivable() != null) {
                totalReceivable = totalReceivable.add(wo.amountReceivable());
            } else if (!"Not Billable".equalsIgnoreCase(wo.billingStatus())) {
                // blank + not explicitly "nothing to bill" -> unknown, not zero
                excludedFromReceivable++;
            }
            // else: Billing Status explicitly says nothing was billed, so a blank
            // receivable here genuinely means zero, not missing data.
        }
        if (excludedFromReceivable > 0) {
            warnings.add(DataQualityWarning.of(DataQualityWarning.Severity.MEDIUM, "MISSING_AMOUNT_RECEIVABLE",
                    excludedFromReceivable + " work order(s) excluded from total receivable: missing amount "
                            + "and Billing Status doesn't indicate nothing was billed", excludedFromReceivable));
        }

        List<AgedReceivable> agedReceivables = workOrders.stream()
                .filter(wo -> wo.amountReceivable() != null && wo.amountReceivable().compareTo(BigDecimal.ZERO) > 0)
                .filter(wo -> wo.probableEndDate() != null)
                .map(wo -> new AgedReceivable(wo.dealName(), wo.customerCode(), wo.amountReceivable(),
                        ChronoUnit.DAYS.between(wo.probableEndDate(), today)))
                .sorted(Comparator.comparingLong(AgedReceivable::daysSinceProbableEnd).reversed())
                .toList();

        return new CollectionsAnalyticsResponse(totalCollected, totalReceivable, agedReceivables, warnings);
    }

    private BigDecimal sum(List<NormalizedWorkOrder> workOrders, java.util.function.Function<NormalizedWorkOrder, BigDecimal> extractor) {
        BigDecimal total = BigDecimal.ZERO;
        for (NormalizedWorkOrder wo : workOrders) {
            total = total.add(extractor.apply(wo));
        }
        return total;
    }
}
