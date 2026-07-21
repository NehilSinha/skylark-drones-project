package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DataQualityWarning;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record BillingAnalyticsResponse(
        Map<String, Long> billingStatusDistribution,
        Map<String, Long> invoiceStatusDistribution,
        BigDecimal totalAmountToBeBilled,
        List<DataQualityWarning> warnings
) {
}
