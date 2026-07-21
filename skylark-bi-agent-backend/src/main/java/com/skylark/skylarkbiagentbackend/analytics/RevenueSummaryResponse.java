package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code billedRevenue}/{@code collectedRevenue}/{@code outstandingReceivable} are
 * {@code null} until the Work Order board is integrated (Phase 5, later batch) —
 * never fabricated as zero. See {@link RevenueAnalyticsService} Javadoc.
 */
public record RevenueSummaryResponse(
        BigDecimal bookedRevenue,
        BigDecimal billedRevenue,
        BigDecimal collectedRevenue,
        BigDecimal outstandingReceivable,
        List<DataQualityWarning> warnings
) {
}
