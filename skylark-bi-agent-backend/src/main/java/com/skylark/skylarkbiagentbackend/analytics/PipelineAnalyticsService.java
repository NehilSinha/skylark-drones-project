package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.config.ForecastProperties;
import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.normalizer.ClosureProbability;
import com.skylark.skylarkbiagentbackend.normalizer.DealStatus;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedDeal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pipeline value, weighted (forecast-style) value, stage funnel, win rate, average
 * deal size, and aging — deterministic Java over normalized Deal Tracker data, no
 * LLM involvement. See PHASE-4-DESIGN.md T5/Group B for the business rules this
 * implements and why several brief-listed capabilities are combined into one
 * snapshot instead of five separate services.
 */
@Component
public class PipelineAnalyticsService {

    private static final String UNSPECIFIED_STAGE = "Unspecified";

    private final DealQueryService dealQueryService;
    private final ForecastProperties forecastProperties;
    private final Clock clock;

    public PipelineAnalyticsService(DealQueryService dealQueryService, ForecastProperties forecastProperties, Clock clock) {
        this.dealQueryService = dealQueryService;
        this.forecastProperties = forecastProperties;
        this.clock = clock;
    }

    public PipelineAnalyticsResponse snapshot(EntityFilter filter) {
        List<NormalizedDeal> deals = dealQueryService.queryAll(filter);
        List<DataQualityWarning> warnings = new ArrayList<>();

        List<NormalizedDeal> openDeals = deals.stream().filter(d -> d.status() == DealStatus.OPEN).toList();
        List<NormalizedDeal> openWithValue = openDeals.stream().filter(d -> d.dealValue() != null).toList();

        BigDecimal totalPipelineValue = sumValues(openWithValue);
        long openMissingValue = openDeals.size() - openWithValue.size();
        if (openMissingValue > 0) {
            warnings.add(DataQualityWarning.of(DataQualityWarning.Severity.MEDIUM, "MISSING_DEAL_VALUE",
                    openMissingValue + " open deal(s) excluded from pipeline value: missing deal value", openMissingValue));
        }

        BigDecimal weightedPipelineValue = weightedValue(openWithValue);
        long missingProbability = openWithValue.stream().filter(d -> d.closureProbability() == null).count();
        if (missingProbability > 0) {
            warnings.add(DataQualityWarning.of(DataQualityWarning.Severity.LOW, "MISSING_CLOSURE_PROBABILITY",
                    missingProbability + " open deal(s) excluded from the weighted forecast value: missing closure probability",
                    missingProbability));
        }

        BigDecimal averageDealSize = openWithValue.isEmpty()
                ? BigDecimal.ZERO
                : totalPipelineValue.divide(BigDecimal.valueOf(openWithValue.size()), 2, RoundingMode.HALF_UP);

        Map<String, StageBucket> stageFunnel = buildStageFunnel(deals);
        double winRate = DealPerformanceMath.winRate(deals);

        long missingCreatedDate = openDeals.stream().filter(d -> d.createdDate() == null).count();
        if (missingCreatedDate > 0) {
            warnings.add(DataQualityWarning.of(DataQualityWarning.Severity.LOW, "MISSING_CREATED_DATE",
                    missingCreatedDate + " open deal(s) excluded from aging buckets: missing created date", missingCreatedDate));
        }
        List<AgingBucket> agingBuckets = buildAgingBuckets(openDeals);

        return new PipelineAnalyticsResponse(
                totalPipelineValue, weightedPipelineValue, stageFunnel, averageDealSize, winRate, agingBuckets, warnings);
    }

    private BigDecimal sumValues(List<NormalizedDeal> deals) {
        BigDecimal total = BigDecimal.ZERO;
        for (NormalizedDeal deal : deals) {
            total = total.add(deal.dealValue());
        }
        return total;
    }

    private BigDecimal weightedValue(List<NormalizedDeal> dealsWithValue) {
        BigDecimal total = BigDecimal.ZERO;
        for (NormalizedDeal deal : dealsWithValue) {
            ClosureProbability probability = deal.closureProbability();
            if (probability == null) {
                continue;
            }
            Double weight = forecastProperties.probabilityWeights().get(probability.name());
            if (weight == null) {
                continue;
            }
            total = total.add(deal.dealValue().multiply(BigDecimal.valueOf(weight)));
        }
        return total;
    }

    private Map<String, StageBucket> buildStageFunnel(List<NormalizedDeal> deals) {
        Map<String, StageBucket> funnel = new LinkedHashMap<>();
        for (NormalizedDeal deal : deals) {
            String stage = deal.dealStage() == null ? UNSPECIFIED_STAGE : deal.dealStage();
            BigDecimal value = deal.dealValue() == null ? BigDecimal.ZERO : deal.dealValue();
            StageBucket existing = funnel.get(stage);
            funnel.put(stage, existing == null
                    ? new StageBucket(stage, 1, value)
                    : new StageBucket(stage, existing.count() + 1, existing.value().add(value)));
        }
        return funnel;
    }

    private List<AgingBucket> buildAgingBuckets(List<NormalizedDeal> openDeals) {
        LocalDate today = LocalDate.now(clock);
        long count0to30 = 0, count31to60 = 0, count61to90 = 0, countOver90 = 0;
        BigDecimal value0to30 = BigDecimal.ZERO, value31to60 = BigDecimal.ZERO,
                value61to90 = BigDecimal.ZERO, valueOver90 = BigDecimal.ZERO;

        for (NormalizedDeal deal : openDeals) {
            if (deal.createdDate() == null) {
                continue;
            }
            long daysOld = ChronoUnit.DAYS.between(deal.createdDate(), today);
            BigDecimal value = deal.dealValue() == null ? BigDecimal.ZERO : deal.dealValue();

            if (daysOld <= 30) {
                count0to30++;
                value0to30 = value0to30.add(value);
            } else if (daysOld <= 60) {
                count31to60++;
                value31to60 = value31to60.add(value);
            } else if (daysOld <= 90) {
                count61to90++;
                value61to90 = value61to90.add(value);
            } else {
                countOver90++;
                valueOver90 = valueOver90.add(value);
            }
        }

        return List.of(
                new AgingBucket("0-30 days", count0to30, value0to30),
                new AgingBucket("31-60 days", count31to60, value31to60),
                new AgingBucket("61-90 days", count61to90, value61to90),
                new AgingBucket("90+ days", countOver90, valueOver90)
        );
    }
}
