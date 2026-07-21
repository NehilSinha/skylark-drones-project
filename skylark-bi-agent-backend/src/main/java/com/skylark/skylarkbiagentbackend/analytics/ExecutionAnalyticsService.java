package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedWorkOrder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Execution status distribution, delayed work orders, and delivery-date variance.
 * See PHASE-4-DESIGN.md T8.
 */
@Component
public class ExecutionAnalyticsService {

    private final WorkOrderQueryService workOrderQueryService;
    private final Clock clock;

    public ExecutionAnalyticsService(WorkOrderQueryService workOrderQueryService, Clock clock) {
        this.workOrderQueryService = workOrderQueryService;
        this.clock = clock;
    }

    public ExecutionAnalyticsResponse snapshot(EntityFilter filter) {
        List<NormalizedWorkOrder> workOrders = workOrderQueryService.queryAll(filter);
        List<DataQualityWarning> warnings = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);

        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        for (NormalizedWorkOrder wo : workOrders) {
            statusDistribution.merge(wo.executionStatus(), 1L, Long::sum);
        }

        long missingEndDate = workOrders.stream().filter(wo -> wo.probableEndDate() == null).count();
        if (missingEndDate > 0) {
            warnings.add(DataQualityWarning.of(DataQualityWarning.Severity.LOW, "MISSING_PROBABLE_END_DATE",
                    missingEndDate + " work order(s) excluded from delay detection: missing probable end date",
                    missingEndDate));
        }

        List<DelayedWorkOrder> delayed = workOrders.stream()
                .filter(wo -> WorkOrderDelayRules.isDelayed(wo, today))
                .map(wo -> new DelayedWorkOrder(wo.dealName(), wo.customerCode(), wo.probableEndDate(),
                        ChronoUnit.DAYS.between(wo.probableEndDate(), today)))
                .toList();

        List<NormalizedWorkOrder> withBothDates = workOrders.stream()
                .filter(wo -> wo.dataDeliveryDate() != null && wo.probableEndDate() != null)
                .toList();
        Double averageVariance = withBothDates.isEmpty() ? null : withBothDates.stream()
                .mapToLong(wo -> ChronoUnit.DAYS.between(wo.probableEndDate(), wo.dataDeliveryDate()))
                .average()
                .orElse(0.0);

        return new ExecutionAnalyticsResponse(statusDistribution, delayed, averageVariance, warnings);
    }
}
