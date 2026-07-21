package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;

import java.math.BigDecimal;
import java.util.List;

public record ForecastResponse(
        BigDecimal weightedForecast,
        BigDecimal bestCase,
        BigDecimal worstCase,
        long excludedDealCount,
        List<DataQualityWarning> warnings
) {
}
