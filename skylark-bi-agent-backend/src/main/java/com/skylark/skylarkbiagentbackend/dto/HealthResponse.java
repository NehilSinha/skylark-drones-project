package com.skylark.skylarkbiagentbackend.dto;

import java.time.Instant;

/**
 * Reports configuration readiness, not a live upstream ping — checking Monday.com
 * on every health poll would be wasteful and would make this app's health depend on
 * a third party's uptime. "Is this deployment configured correctly" is the
 * meaningful signal for a Docker health check / ops dashboard.
 */
public record HealthResponse(
        String status,
        Instant timestamp,
        boolean mondayApiConfigured,
        boolean mondayBoardsConfigured,
        boolean llmConfigured
) {
}
