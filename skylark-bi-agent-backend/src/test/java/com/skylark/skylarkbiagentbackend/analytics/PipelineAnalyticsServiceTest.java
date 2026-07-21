package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.config.ForecastProperties;
import com.skylark.skylarkbiagentbackend.normalizer.ClosureProbability;
import com.skylark.skylarkbiagentbackend.normalizer.DealStatus;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedDeal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Hand-computed fixture per PHASE-4-DESIGN.md T5 test strategy: a deliberate mix of
 * Won/Dead/Open/On-Hold status and blank-revenue/blank-probability rows, asserting
 * exact totals rather than just "some result comes back."
 */
class PipelineAnalyticsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 21);

    private DealQueryService dealQueryService;
    private PipelineAnalyticsService service;

    @BeforeEach
    void setUp() {
        dealQueryService = Mockito.mock(DealQueryService.class);
        Clock clock = Clock.fixed(TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        ForecastProperties forecastProperties = new ForecastProperties(Map.of("HIGH", 0.8, "MEDIUM", 0.5, "LOW", 0.2));
        service = new PipelineAnalyticsService(dealQueryService, forecastProperties, clock);
    }

    @Test
    void snapshot_computesExactFiguresAcrossMixedDealSet() {
        NormalizedDeal openHighValue = deal("A", DealStatus.OPEN, new BigDecimal("100000"), ClosureProbability.HIGH,
                "B. Sales Qualified Leads", TODAY.minusDays(10));
        NormalizedDeal openBlankValue = deal("B", DealStatus.OPEN, null, ClosureProbability.MEDIUM,
                "B. Sales Qualified Leads", TODAY.minusDays(45));
        NormalizedDeal openBlankProbability = deal("C", DealStatus.OPEN, new BigDecimal("50000"), null,
                "C. Demo Done", TODAY.minusDays(100));
        NormalizedDeal won = deal("D", DealStatus.WON, new BigDecimal("200000"), null, "G. Project Won", null);
        NormalizedDeal dead = deal("E", DealStatus.DEAD, new BigDecimal("30000"), null, "L. Project Lost", null);
        NormalizedDeal onHold = deal("F", DealStatus.ON_HOLD, new BigDecimal("10000"), null, "M. Projects On Hold", null);

        when(dealQueryService.queryAll(any())).thenReturn(
                List.of(openHighValue, openBlankValue, openBlankProbability, won, dead, onHold));

        PipelineAnalyticsResponse response = service.snapshot(null);

        assertThat(response.totalPipelineValue()).isEqualByComparingTo("150000");
        assertThat(response.weightedPipelineValue()).isEqualByComparingTo("80000"); // only A: 100000 * 0.8
        assertThat(response.averageDealSize()).isEqualByComparingTo("75000.00"); // 150000 / 2 valued open deals

        assertThat(response.winRate()).isEqualTo(0.5); // 1 won / (1 won + 1 dead), on-hold excluded

        assertThat(response.stageFunnel()).hasSize(5);
        assertThat(response.stageFunnel().get("B. Sales Qualified Leads").count()).isEqualTo(2);
        assertThat(response.stageFunnel().get("B. Sales Qualified Leads").value()).isEqualByComparingTo("100000");
        assertThat(response.stageFunnel().get("G. Project Won").value()).isEqualByComparingTo("200000");

        assertThat(response.agingBuckets()).hasSize(4);
        assertThat(bucket(response, "0-30 days").count()).isEqualTo(1);
        assertThat(bucket(response, "31-60 days").count()).isEqualTo(1);
        assertThat(bucket(response, "90+ days").count()).isEqualTo(1);
        assertThat(bucket(response, "61-90 days").count()).isZero();

        assertThat(response.warnings()).hasSize(2);
        assertThat(response.warnings())
                .anyMatch(w -> "MISSING_DEAL_VALUE".equals(w.code()) && w.affectedRecordCount() == 1)
                .anyMatch(w -> "MISSING_CLOSURE_PROBABILITY".equals(w.code()) && w.affectedRecordCount() == 1);
    }

    @Test
    void snapshot_noOpenDeals_returnsZeroesNotAnException() {
        NormalizedDeal won = deal("D", DealStatus.WON, new BigDecimal("200000"), null, "G. Project Won", null);
        when(dealQueryService.queryAll(any())).thenReturn(List.of(won));

        PipelineAnalyticsResponse response = service.snapshot(null);

        assertThat(response.totalPipelineValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.averageDealSize()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.winRate()).isEqualTo(1.0); // 1 won / (1 won + 0 dead)
    }

    private AgingBucket bucket(PipelineAnalyticsResponse response, String label) {
        return response.agingBuckets().stream()
                .filter(b -> b.label().equals(label))
                .findFirst()
                .orElseThrow();
    }

    private NormalizedDeal deal(String id, DealStatus status, BigDecimal value, ClosureProbability probability,
                                 String stage, LocalDate createdDate) {
        return new NormalizedDeal(id, "Deal " + id, "OWNER_001", "COMPANY001", status, null, probability,
                value, null, stage, null, "Mining", createdDate);
    }
}
