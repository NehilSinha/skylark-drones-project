package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedWorkOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Billing/invoice status breakdown and amount-to-be-billed. See
 * PHASE-4-DESIGN.md T10 — status distribution keys use the canonical value from
 * {@code WorkOrderNormalizer} (e.g. {@code Billed}/{@code BIlled} collapse to one
 * bucket), and blank Billing Status reports as its own {@code "Not Set"} bucket
 * rather than being merged into {@code Not Billable} or dropped.
 */
@Component
public class BillingAnalyticsService {

    private final WorkOrderQueryService workOrderQueryService;

    public BillingAnalyticsService(WorkOrderQueryService workOrderQueryService) {
        this.workOrderQueryService = workOrderQueryService;
    }

    public BillingAnalyticsResponse snapshot(EntityFilter filter) {
        List<NormalizedWorkOrder> workOrders = workOrderQueryService.queryAll(filter);
        List<DataQualityWarning> warnings = new ArrayList<>();

        Map<String, Long> billingStatusDistribution = new LinkedHashMap<>();
        Map<String, Long> invoiceStatusDistribution = new LinkedHashMap<>();
        for (NormalizedWorkOrder wo : workOrders) {
            billingStatusDistribution.merge(wo.billingStatus(), 1L, Long::sum);
            invoiceStatusDistribution.merge(wo.invoiceStatus(), 1L, Long::sum);
        }

        List<NormalizedWorkOrder> withAmount = workOrders.stream()
                .filter(wo -> wo.amountToBeBilledExclGst() != null)
                .toList();
        long missingAmount = workOrders.size() - withAmount.size();
        if (missingAmount > 0) {
            warnings.add(DataQualityWarning.of(DataQualityWarning.Severity.MEDIUM, "MISSING_AMOUNT_TO_BE_BILLED",
                    missingAmount + " work order(s) excluded from total amount to be billed: missing amount",
                    missingAmount));
        }

        BigDecimal total = BigDecimal.ZERO;
        for (NormalizedWorkOrder wo : withAmount) {
            total = total.add(wo.amountToBeBilledExclGst());
        }

        return new BillingAnalyticsResponse(billingStatusDistribution, invoiceStatusDistribution, total, warnings);
    }
}
