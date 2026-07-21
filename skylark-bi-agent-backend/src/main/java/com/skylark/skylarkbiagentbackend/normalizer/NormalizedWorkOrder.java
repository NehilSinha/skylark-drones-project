package com.skylark.skylarkbiagentbackend.normalizer;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A Work Order Tracker board item after normalization. {@code executionStatus},
 * {@code billingStatus}, and {@code invoiceStatus} are canonicalized free text
 * (never a rigid enum — the source data has too many real, meaningful variants,
 * same precedent as {@code NormalizedDeal.dealStage}), defaulting to
 * {@code "Not Set"} when blank rather than {@code null}, so they're always safe as
 * a grouping key. Monetary/quantity fields are {@code null} when blank/unparseable
 * — never coerced to zero. There is deliberately no field for the raw
 * "Collection status" column: Phase 1 found it contains month names instead of a
 * status enum in the real data, so "collected" is derived from
 * {@code collectedAmount} instead (see {@code CollectionsAnalyticsService}).
 */
public record NormalizedWorkOrder(
        String itemId,
        String dealName,
        String customerCode,
        String ownerCode,
        String sector,
        String executionStatus,
        LocalDate probableEndDate,
        LocalDate dataDeliveryDate,
        String billingStatus,
        String invoiceStatus,
        BigDecimal amountToBeBilledExclGst,
        BigDecimal collectedAmount,
        BigDecimal amountReceivable,
        BigDecimal quantityAsPerPo,
        BigDecimal quantityBilled
) {
}
