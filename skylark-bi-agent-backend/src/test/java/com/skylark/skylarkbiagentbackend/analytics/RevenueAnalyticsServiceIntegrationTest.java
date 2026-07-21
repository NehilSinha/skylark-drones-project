package com.skylark.skylarkbiagentbackend.analytics;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.skylark.skylarkbiagentbackend.client.monday.MondayGraphQLClient;
import com.skylark.skylarkbiagentbackend.client.monday.MondayQueryBuilder;
import com.skylark.skylarkbiagentbackend.client.monday.MondayRateLimiter;
import com.skylark.skylarkbiagentbackend.config.DealColumnMapping;
import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.normalizer.DealNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves RevenueAnalyticsService through the real Monday fetch -> normalize chain,
 * reusing the walking-skeleton WireMock pattern.
 */
class RevenueAnalyticsServiceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    private static final DealColumnMapping MAPPING = new DealColumnMapping(
            "owner_code", "client_code", "status", "close_date", "probability",
            "deal_value", "tentative_close_date", "stage", "product", "sector", "created_date");

    private RevenueAnalyticsService revenueAnalyticsService;

    @BeforeEach
    void setUp() {
        MondayProperties properties = new MondayProperties(
                new MondayProperties.Api(wireMock.baseUrl(), "test-token", 2000, 2000),
                new MondayProperties.Boards("1", "2"),
                new MondayProperties.Pagination(100, 10),
                new MondayProperties.RateLimit(1000),
                new MondayProperties.Retry(3, 10, 1.0));

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.api().baseUrl())
                .defaultHeader("Authorization", properties.api().token())
                .build();

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1, Map.of()));
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(1L);
        retryTemplate.setBackOffPolicy(backOff);

        MondayGraphQLClient client = new MondayGraphQLClient(
                restClient, retryTemplate, new MondayRateLimiter(properties), new MondayQueryBuilder(), properties);

        DealQueryService dealQueryService = new DealQueryService(client, properties, new DealNormalizer(), MAPPING);
        revenueAnalyticsService = new RevenueAnalyticsService(dealQueryService);
    }

    @Test
    void summary_endToEndThroughRealFetchNormalizeChain() {
        wireMock.stubFor(post(urlEqualTo("/")).willReturn(okJson("""
                {"data":{"boards":[{"items_page":{"cursor":null,"items":[
                    {"id":"1","name":"Naruto","column_values":[
                        {"id":"status","text":"Won","value":null},
                        {"id":"deal_value","text":"489360","value":null},
                        {"id":"close_date","text":"2026-02-26","value":null}
                    ]},
                    {"id":"2","name":"Sasuke","column_values":[
                        {"id":"status","text":"Open","value":null},
                        {"id":"deal_value","text":"17616960","value":null}
                    ]}
                ]}}]}}
                """)));

        RevenueSummaryResponse response = revenueAnalyticsService.summary(null);

        assertThat(response.bookedRevenue()).isEqualByComparingTo("489360"); // only the Won deal
        assertThat(response.billedRevenue()).isNull();
        assertThat(response.warnings()).anyMatch(w -> "WORK_ORDER_DATA_UNAVAILABLE".equals(w.code()));
    }
}
