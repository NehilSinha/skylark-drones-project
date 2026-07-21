package com.skylark.skylarkbiagentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Application-level settings that don't belong to a specific integration. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Cache cache, Memory memory) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Cache(int analyticsTtlSeconds) {
    }

    public record Memory(long promptHistoryTtlSeconds, int maxContextTurns) {
    }
}
