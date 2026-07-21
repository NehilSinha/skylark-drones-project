package com.skylark.skylarkbiagentbackend.analytics;

import java.math.BigDecimal;

/**
 * {@code daysSinceProbableEnd} substitutes for PHASE-4-DESIGN.md's
 * {@code daysSinceExpectedBilling}: "Expected Billing Month" is a Monday
 * month-picker column, not a precise date, and isn't modeled — probable end date
 * is the closest reliable proxy for "when this should have been billed."
 */
public record AgedReceivable(String dealName, String customerCode, BigDecimal amount, long daysSinceProbableEnd) {
}
