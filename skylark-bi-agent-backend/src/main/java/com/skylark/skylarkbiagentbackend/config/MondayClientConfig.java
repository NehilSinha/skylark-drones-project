package com.skylark.skylarkbiagentbackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class MondayClientConfig {

    private static final Logger log = LoggerFactory.getLogger(MondayClientConfig.class);

    @Bean
    public RestClient mondayRestClient(MondayProperties properties) {
        if (!properties.api().isConfigured()) {
            log.warn("MONDAY_API_TOKEN is not set — Monday-backed features will fail until it is configured.");
        }
        if (!properties.boards().isConfigured()) {
            log.warn("Monday board IDs are not set — set MONDAY_DEALS_BOARD_ID and MONDAY_WORK_ORDERS_BOARD_ID.");
        }

        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofMillis(properties.api().connectTimeoutMs()))
                .withReadTimeout(Duration.ofMillis(properties.api().readTimeoutMs()));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        return RestClient.builder()
                .baseUrl(properties.api().baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    // Attached per-request (not as a static default header) so a token
                    // rotated at runtime via config refresh is always picked up.
                    if (properties.api().isConfigured()) {
                        request.getHeaders().set("Authorization", properties.api().token());
                    }
                    // Method/URI only — the Authorization header and request body (which
                    // may contain business data) are never written to logs.
                    log.debug("Monday.com request: {} {}", request.getMethod(), request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }
}
