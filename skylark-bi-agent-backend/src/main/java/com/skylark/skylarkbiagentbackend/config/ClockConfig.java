package com.skylark.skylarkbiagentbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * A single injectable {@link Clock}, used everywhere "today" matters
 * ({@code DateExpressionResolver}, deal-aging calculations) so tests can supply a
 * fixed clock instead of depending on wall-clock time.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
