package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.normalizer.DealStatus;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedDeal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The composite "what's our revenue" answer from PHASE-4-DESIGN.md T12. The full
 * design composes booked (Deal board), billed, and collected (Work Order board)
 * revenue via {@code PipelineAnalyticsService}/{@code BillingAnalyticsService}/
 * {@code CollectionsAnalyticsService}. Only the Deal board side is built so far —
 * {@code BillingAnalyticsService}/{@code CollectionsAnalyticsService} and
 * {@code WorkOrderNormalizer} don't exist yet (a later Phase 5 batch). This service
 * therefore reports {@code bookedRevenue} for real and leaves the other three
 * figures {@code null} with an explanatory warning — never a fabricated zero —
 * rather than waiting for the whole design to be buildable before shipping any of
 * it. The response shape already matches the full design, so wiring in the Work
 * Order side later is additive, not a breaking change.
 *
 * <p>Business rule: "booked" is inherently Won-scoped, so this service ignores any
 * {@code dealStatuses} filter the caller supplies (a status filter alongside a
 * booked-revenue question is a contradiction in terms, not a valid narrowing) and
 * filters date range against each deal's actual {@code closeDate} rather than
 * {@code tentativeCloseDate} — a closed deal's "tentative" close date is no longer
 * the relevant date, unlike in {@link PipelineAnalyticsService} and
 * {@link ForecastService} where deals are still open.
 */
@Component
public class RevenueAnalyticsService {

    private final DealQueryService dealQueryService;

    public RevenueAnalyticsService(DealQueryService dealQueryService) {
        this.dealQueryService = dealQueryService;
    }

    public RevenueSummaryResponse summary(EntityFilter filter) {
        List<DataQualityWarning> warnings = new ArrayList<>();

        EntityFilter dealFilter = filter == null
                ? null
                : new EntityFilter(filter.sectors(), filter.owners(), filter.clients(), null, null);
        DateRange closeDateRange = filter == null ? null : filter.dateRange();

        List<NormalizedDeal> wonDeals = dealQueryService.queryAll(dealFilter).stream()
                .filter(d -> d.status() == DealStatus.WON)
                .filter(d -> matchesCloseDate(d, closeDateRange))
                .toList();

        List<NormalizedDeal> wonWithValue = wonDeals.stream().filter(d -> d.dealValue() != null).toList();
        long missingValueCount = wonDeals.size() - wonWithValue.size();
        if (missingValueCount > 0) {
            warnings.add(DataQualityWarning.of(DataQualityWarning.Severity.MEDIUM, "MISSING_DEAL_VALUE",
                    missingValueCount + " won deal(s) excluded from booked revenue: missing deal value",
                    missingValueCount));
        }

        BigDecimal bookedRevenue = BigDecimal.ZERO;
        for (NormalizedDeal deal : wonWithValue) {
            bookedRevenue = bookedRevenue.add(deal.dealValue());
        }

        warnings.add(DataQualityWarning.of(DataQualityWarning.Severity.INFO, "WORK_ORDER_DATA_UNAVAILABLE",
                "Billed revenue, collected revenue, and outstanding receivables are not available yet: "
                        + "Work Order board integration has not been built.", 0));

        return new RevenueSummaryResponse(bookedRevenue, null, null, null, warnings);
    }

    private boolean matchesCloseDate(NormalizedDeal deal, DateRange dateRange) {
        if (dateRange == null) {
            return true;
        }
        if (deal.closeDate() == null) {
            return false;
        }
        return !deal.closeDate().isBefore(dateRange.start()) && !deal.closeDate().isAfter(dateRange.end());
    }
}
