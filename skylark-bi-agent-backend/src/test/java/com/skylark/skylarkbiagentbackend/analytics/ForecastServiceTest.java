package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.config.ForecastProperties;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.exception.ValidationException;
import com.skylark.skylarkbiagentbackend.normalizer.ClosureProbability;
import com.skylark.skylarkbiagentbackend.normalizer.DealStatus;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedDeal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ForecastServiceTest {

    private static final DateRange HORIZON = new DateRange(
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30), "this quarter");

    private DealQueryService dealQueryService;
    private ForecastService service;

    @BeforeEach
    void setUp() {
        dealQueryService = Mockito.mock(DealQueryService.class);
        ForecastProperties forecastProperties = new ForecastProperties(Map.of("HIGH", 0.8, "MEDIUM", 0.5, "LOW", 0.2));
        service = new ForecastService(dealQueryService, forecastProperties);
    }

    @Test
    void forecast_computesExactFiguresAcrossMixedEligibility() {
        NormalizedDeal highInHorizon = deal("A", DealStatus.OPEN, new BigDecimal("100000"), ClosureProbability.HIGH,
                LocalDate.of(2026, 8, 15));
        NormalizedDeal mediumInHorizon = deal("B", DealStatus.OPEN, new BigDecimal("200000"), ClosureProbability.MEDIUM,
                LocalDate.of(2026, 9, 1));
        NormalizedDeal blankProbabilityInHorizon = deal("C", DealStatus.OPEN, new BigDecimal("50000"), null,
                LocalDate.of(2026, 7, 10));
        NormalizedDeal blankValueInHorizon = deal("D", DealStatus.OPEN, null, ClosureProbability.HIGH,
                LocalDate.of(2026, 8, 1));
        NormalizedDeal outsideHorizon = deal("E", DealStatus.OPEN, new BigDecimal("80000"), ClosureProbability.HIGH,
                LocalDate.of(2026, 10, 15));
        NormalizedDeal wonDeal = deal("F", DealStatus.WON, new BigDecimal("999999"), ClosureProbability.HIGH,
                LocalDate.of(2026, 8, 1));
        NormalizedDeal noCloseDate = deal("G", DealStatus.OPEN, new BigDecimal("30000"), ClosureProbability.HIGH, null);

        when(dealQueryService.queryAll(any())).thenReturn(List.of(
                highInHorizon, mediumInHorizon, blankProbabilityInHorizon, blankValueInHorizon,
                outsideHorizon, wonDeal, noCloseDate));

        ForecastResponse response = service.forecast(HORIZON, null);

        assertThat(response.bestCase()).isEqualByComparingTo("350000"); // A + B + C
        assertThat(response.worstCase()).isEqualByComparingTo("100000"); // only A is HIGH confidence with a value
        assertThat(response.weightedForecast()).isEqualByComparingTo("180000"); // 100000*0.8 + 200000*0.5
        assertThat(response.excludedDealCount()).isEqualTo(1); // C: blank probability

        assertThat(response.warnings()).hasSize(2);
        assertThat(response.warnings())
                .anyMatch(w -> "MISSING_DEAL_VALUE".equals(w.code()) && w.affectedRecordCount() == 1)
                .anyMatch(w -> "MISSING_CLOSURE_PROBABILITY".equals(w.code()) && w.affectedRecordCount() == 1);
    }

    @Test
    void forecast_noEligibleDeals_returnsZeroesNotAnException() {
        when(dealQueryService.queryAll(any())).thenReturn(List.of());

        ForecastResponse response = service.forecast(HORIZON, null);

        assertThat(response.bestCase()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.worstCase()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.weightedForecast()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.warnings()).isEmpty();
    }

    @Test
    void forecast_nullHorizon_throwsValidationException() {
        assertThatThrownBy(() -> service.forecast(null, null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void forecast_invertedHorizon_throwsValidationException() {
        DateRange inverted = new DateRange(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 7, 1), "invalid");
        assertThatThrownBy(() -> service.forecast(inverted, null)).isInstanceOf(ValidationException.class);
    }

    private NormalizedDeal deal(String id, DealStatus status, BigDecimal value, ClosureProbability probability,
                                 LocalDate tentativeCloseDate) {
        return new NormalizedDeal(id, "Deal " + id, "OWNER_001", "COMPANY001", status, null, probability,
                value, tentativeCloseDate, "B. Sales Qualified Leads", null, "Mining", null);
    }
}
