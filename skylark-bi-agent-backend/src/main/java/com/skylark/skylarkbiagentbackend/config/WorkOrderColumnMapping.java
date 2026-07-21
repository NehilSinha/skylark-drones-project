package com.skylark.skylarkbiagentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Maps logical Work Order Tracker fields to Monday column IDs — same rationale and
 * blank-safe default as {@link DealColumnMapping}.
 */
@ConfigurationProperties(prefix = "monday.column-mapping.work-orders")
public record WorkOrderColumnMapping(
        String customerCode,
        String ownerCode,
        String sector,
        String executionStatus,
        String probableEndDate,
        String dataDeliveryDate,
        String billingStatus,
        String invoiceStatus,
        String amountToBeBilled,
        String collectedAmount,
        String amountReceivable,
        String quantityAsPerPo,
        String quantityBilled
) {
}
