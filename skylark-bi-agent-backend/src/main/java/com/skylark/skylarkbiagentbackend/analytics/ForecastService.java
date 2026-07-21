package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.config.ForecastProperties;
import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.exception.ValidationException;
import com.skylark.skylarkbiagentbackend.normalizer.ClosureProbability;
import com.skylark.skylarkbiagentbackend.normalizer.DealStatus;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedDeal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Probability-weighted revenue forecast over a horizon. See PHASE-4-DESIGN.md T6
 * for the business rules this implements, most notably: a blank Closure
 * Probability excludes a deal from {@code weightedForecast} but not from
 * {@code bestCase} — 258 of 346 real deals have a blank probability, and silently
 * dropping them from every figure would understate the pipeline far more than the
 * forecast tool is meant to.
 */
@Component
public class ForecastService {

    private final DealQueryService dealQueryService;
    private final ForecastProperties forecastProperties;

    public ForecastService(DealQueryService dealQueryService, ForecastProperties forecastProperties) {
        this.dealQueryService = dealQueryService;
        this.forecastProperties = forecastProperties;
    }

    public ForecastResponse forecast(DateRange horizon, EntityFilter filter) {
        if (horizon == null) {
            throw new ValidationException("A forecast horizon is required");
        }
        if (horizon.start().isAfter(horizon.end())) {
            throw new ValidationException("Forecast horizon start must not be after its end");
        }

        List<NormalizedDeal> deals = dealQueryService.queryAll(filter);
        List<DataQualityWarning> warnings = new ArrayList<>();

        List<NormalizedDeal> eligible = deals.stream()
                .filter(d -> d.status() == DealStatus.OPEN)
                .filter(d -> d.tentativeCloseDate() != null)
                .filter(d -> !d.tentativeCloseDate().isBefore(horizon.start()) && !d.tentativeCloseDate().isAfter(horizon.end()))
                .toList();

        List<NormalizedDeal> eligibleWithValue = eligible.stream().filter(d -> d.dealValue() != null).toList();
        long missingValueCount = eligible.size() - eligibleWithValue.size();
        if (missingValueCount > 0) {
            warnings.add(DataQualityWarning.of(DataQualityWarning.Severity.MEDIUM, "MISSING_DEAL_VALUE",
                    missingValueCount + " deal(s) in the forecast horizon excluded entirely: missing deal value",
                    missingValueCount));
        }

        BigDecimal bestCase = sumValues(eligibleWithValue);

        List<NormalizedDeal> highConfidence = eligibleWithValue.stream()
                .filter(d -> d.closureProbability() == ClosureProbability.HIGH)
                .toList();
        BigDecimal worstCase = sumValues(highConfidence);

        List<NormalizedDeal> weighable = eligibleWithValue.stream()
                .filter(d -> d.closureProbability() != null)
                .toList();
        long excludedDealCount = eligibleWithValue.size() - weighable.size();
        if (excludedDealCount > 0) {
            warnings.add(DataQualityWarning.of(DataQualityWarning.Severity.LOW, "MISSING_CLOSURE_PROBABILITY",
                    excludedDealCount + " deal(s) with a known value excluded from the weighted forecast "
                            + "(still counted in best case): missing closure probability",
                    excludedDealCount));
        }

        BigDecimal weightedForecast = BigDecimal.ZERO;
        for (NormalizedDeal deal : weighable) {
            Double weight = forecastProperties.probabilityWeights().get(deal.closureProbability().name());
            if (weight == null) {
                continue;
            }
            weightedForecast = weightedForecast.add(deal.dealValue().multiply(BigDecimal.valueOf(weight)));
        }

        return new ForecastResponse(weightedForecast, bestCase, worstCase, excludedDealCount, warnings);
    }

    private BigDecimal sumValues(List<NormalizedDeal> deals) {
        BigDecimal total = BigDecimal.ZERO;
        for (NormalizedDeal deal : deals) {
            total = total.add(deal.dealValue());
        }
        return total;
    }
}
