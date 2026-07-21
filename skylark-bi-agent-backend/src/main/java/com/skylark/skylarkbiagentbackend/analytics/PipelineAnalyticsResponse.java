package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PipelineAnalyticsResponse(
        BigDecimal totalPipelineValue,
        BigDecimal weightedPipelineValue,
        Map<String, StageBucket> stageFunnel,
        BigDecimal averageDealSize,
        double winRate,
        List<AgingBucket> agingBuckets,
        List<DataQualityWarning> warnings
) {
}
