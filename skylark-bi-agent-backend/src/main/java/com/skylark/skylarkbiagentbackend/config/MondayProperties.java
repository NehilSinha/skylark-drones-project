package com.skylark.skylarkbiagentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Environment-driven Monday.com wiring. Every field is sourced from
 * {@code application.yml} placeholders, which in turn read from env vars — no board
 * ID or credential is ever a literal in code (see ADR-001, "Monday API Interaction").
 */
@ConfigurationProperties(prefix = "monday")
public record MondayProperties(
        Api api,
        Boards boards,
        Pagination pagination,
        RateLimit rateLimit,
        Retry retry
) {

    public record Api(String baseUrl, String token, int connectTimeoutMs, int readTimeoutMs) {
        public boolean isConfigured() {
            return token != null && !token.isBlank();
        }
    }

    public record Boards(String dealsId, String workOrdersId) {
        public boolean isConfigured() {
            return dealsId != null && !dealsId.isBlank()
                    && workOrdersId != null && !workOrdersId.isBlank();
        }
    }

    public record Pagination(int pageSize, int maxPages) {
    }

    public record RateLimit(int permitsPerMinute) {
    }

    public record Retry(int maxAttempts, long initialBackoffMs, double backoffMultiplier) {
    }
}
