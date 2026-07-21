package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.normalizer.NormalizedWorkOrder;

import java.time.LocalDate;
import java.util.Set;

/**
 * The "is this work order delayed" rule, shared by {@code ExecutionAnalyticsService}
 * and every owner/client/sector rollup that counts delayed work orders — extracted
 * because it was about to be duplicated across three-plus services, not on
 * spec­ulation. See PHASE-4-DESIGN.md T8: recurring projects ({@code "Executed
 * until current month"}) are excluded since an ongoing engagement without a fixed
 * end isn't "late" the way a one-time project is.
 */
public final class WorkOrderDelayRules {

    private static final Set<String> EXCLUDED_FROM_DELAY = Set.of("Completed", "Executed until current month");

    private WorkOrderDelayRules() {
    }

    public static boolean isDelayed(NormalizedWorkOrder workOrder, LocalDate today) {
        return workOrder.probableEndDate() != null
                && workOrder.probableEndDate().isBefore(today)
                && !EXCLUDED_FROM_DELAY.contains(workOrder.executionStatus());
    }
}
