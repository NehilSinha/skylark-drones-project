package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;

import java.math.BigDecimal;
import java.util.List;

public record CollectionsAnalyticsResponse(
        BigDecimal totalCollected,
        BigDecimal totalReceivable,
        List<AgedReceivable> agedReceivables,
        List<DataQualityWarning> warnings
) {
}
