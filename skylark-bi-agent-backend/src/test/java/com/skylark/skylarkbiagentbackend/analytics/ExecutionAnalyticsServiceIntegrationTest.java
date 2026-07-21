package com.skylark.skylarkbiagentbackend.analytics;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.skylark.skylarkbiagentbackend.client.monday.MondayGraphQLClient;
import com.skylark.skylarkbiagentbackend.client.monday.MondayQueryBuilder;
import com.skylark.skylarkbiagentbackend.client.monday.MondayRateLimiter;
import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.config.WorkOrderColumnMapping;
import com.skylark.skylarkbiagentbackend.normalizer.WorkOrderNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves ExecutionAnalyticsService through the real Monday fetch -> normalize ->
 * filter chain for the Work Order board, mirroring ForecastServiceIntegrationTest's
 * pattern for the Deal board.
 */
class ExecutionAnalyticsServiceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    private static final WorkOrderColumnMapping MAPPING = new WorkOrderColumnMapping(
            "customer_code", "owner_code", "sector", "execution_status", "probable_end_date",
            "data_delivery_date", "billing_status", "invoice_status", "amount_to_be_billed",
            "collected_amount", "amount_receivable", "quantity_as_per_po", "quantity_billed");

    private ExecutionAnalyticsService executionAnalyticsService;

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

        WorkOrderQueryService workOrderQueryService =
                new WorkOrderQueryService(client, properties, new WorkOrderNormalizer(), MAPPING);
        Clock clock = Clock.fixed(LocalDate.of(2026, 7, 21).atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        executionAnalyticsService = new ExecutionAnalyticsService(workOrderQueryService, clock);
    }

    @Test
    void snapshot_endToEndThroughRealFetchNormalizeChain() {
        wireMock.stubFor(post(urlEqualTo("/")).willReturn(okJson("""
                {"data":{"boards":[{"items_page":{"cursor":null,"items":[
                    {"id":"1","name":"Overdue Job","column_values":[
                        {"id":"execution_status","text":"Not Started","value":null},
                        {"id":"probable_end_date","text":"2026-07-01","value":null}
                    ]},
                    {"id":"2","name":"Completed Job","column_values":[
                        {"id":"execution_status","text":"Completed","value":null},
                        {"id":"probable_end_date","text":"2026-06-01","value":null}
                    ]}
                ]}}]}}
                """)));

        ExecutionAnalyticsResponse response = executionAnalyticsService.snapshot(null);

        assertThat(response.delayed()).hasSize(1);
        assertThat(response.delayed().get(0).dealName()).isEqualTo("Overdue Job");
        assertThat(response.statusDistribution()).containsEntry("Not Started", 1L).containsEntry("Completed", 1L);
    }
}
