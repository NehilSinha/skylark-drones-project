package com.skylark.skylarkbiagentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Tunables for {@code EntityResolutionService}. Prefixes are configurable (not
 * hardcoded) per ADR-001 — the {@code COMPANY089}/{@code WOCOMPANY_089} namespace
 * mismatch found in Phase 1 is real source data, not a universal constant.
 */
@ConfigurationProperties(prefix = "entity-resolution")
public record EntityResolutionProperties(
        List<String> clientCodePrefixesToStrip,
        double fuzzyAutoAcceptThreshold,
        double fuzzyPossibleThreshold
) {
}
