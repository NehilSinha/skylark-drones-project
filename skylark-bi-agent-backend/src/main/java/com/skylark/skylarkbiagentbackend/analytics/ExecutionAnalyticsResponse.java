package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;

import java.util.List;
import java.util.Map;

public record ExecutionAnalyticsResponse(
        Map<String, Long> statusDistribution,
        List<DelayedWorkOrder> delayed,
        Double averageDeliveryVarianceDays,
        List<DataQualityWarning> warnings
) {
}
