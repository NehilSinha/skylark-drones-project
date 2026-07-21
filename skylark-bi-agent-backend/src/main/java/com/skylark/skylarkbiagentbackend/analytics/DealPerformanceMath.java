package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.normalizer.DealStatus;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedDeal;

import java.math.BigDecimal;
import java.util.List;

/**
 * Win-rate and open-pipeline-value math, shared by {@code PipelineAnalyticsService}
 * and every owner/client/sector rollup — extracted once the same two calculations
 * were about to appear in three more services this batch.
 */
public final class DealPerformanceMath {

    private DealPerformanceMath() {
    }

    /** Won / (Won + Dead); Open and On Hold deals are excluded from the denominator — an in-flight deal isn't a loss. */
    public static double winRate(List<NormalizedDeal> deals) {
        long won = deals.stream().filter(d -> d.status() == DealStatus.WON).count();
        long dead = deals.stream().filter(d -> d.status() == DealStatus.DEAD).count();
        long decided = won + dead;
        return decided == 0 ? 0.0 : (double) won / decided;
    }

    /** Sum of deal value for Open deals with a known value — blank-value deals are excluded, never zero-filled. */
    public static BigDecimal openPipelineValue(List<NormalizedDeal> deals) {
        BigDecimal total = BigDecimal.ZERO;
        for (NormalizedDeal deal : deals) {
            if (deal.status() == DealStatus.OPEN && deal.dealValue() != null) {
                total = total.add(deal.dealValue());
            }
        }
        return total;
    }
}
