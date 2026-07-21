package com.skylark.skylarkbiagentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Closure-probability-to-weight mapping, keyed by {@code ClosureProbability.name()}
 * (HIGH/MEDIUM/LOW). Confirmed 0.8/0.5/0.2 (PHASE-4-DESIGN.md §6) — kept
 * configurable rather than a code constant so it can be recalibrated later against
 * real Won/Dead outcome data without a redeploy.
 */
@ConfigurationProperties(prefix = "app.forecast")
public record ForecastProperties(Map<String, Double> probabilityWeights) {
}
