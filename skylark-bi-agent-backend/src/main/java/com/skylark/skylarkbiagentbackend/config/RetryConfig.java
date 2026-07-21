package com.skylark.skylarkbiagentbackend.config;

import com.skylark.skylarkbiagentbackend.client.monday.TransientMondayFailureException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

/**
 * Provides the {@link RetryTemplate} the Monday client uses. A {@code RetryTemplate}
 * (rather than the {@code @Retryable} annotation) is used because the retry policy's
 * max attempts and backoff come from {@link MondayProperties}, which are only known
 * at runtime — and it avoids an AOP-proxying dependency for a single call site.
 * If a future use case wants annotation-based retry, add {@code spring-boot-starter-aop}
 * and {@code @EnableRetry} back at that point.
 */
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate mondayRetryTemplate(MondayProperties properties) {
        MondayProperties.Retry retry = properties.retry();

        // Only TransientMondayFailureException triggers a retry attempt; every other
        // exception (auth failures, malformed queries) fails on the first attempt.
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                Math.max(1, retry.maxAttempts()),
                Map.of(TransientMondayFailureException.class, true));

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(Math.max(1, retry.initialBackoffMs()));
        backOffPolicy.setMultiplier(retry.backoffMultiplier() > 1 ? retry.backoffMultiplier() : 2.0);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);
        return template;
    }
}
