package com.skylark.skylarkbiagentbackend.analytics;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.skylark.skylarkbiagentbackend.client.monday.MondayGraphQLClient;
import com.skylark.skylarkbiagentbackend.client.monday.MondayQueryBuilder;
import com.skylark.skylarkbiagentbackend.client.monday.MondayRateLimiter;
import com.skylark.skylarkbiagentbackend.config.DealColumnMapping;
import com.skylark.skylarkbiagentbackend.config.ForecastProperties;
import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.normalizer.DealNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves ForecastService through the real Monday fetch -> normalize -> filter
 * chain (not a mocked DealQueryService), reusing the walking-skeleton WireMock
 * pattern from DealQueryServiceTest/MondayGraphQLClientTest.
 */
class ForecastServiceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    private static final DealColumnMapping MAPPING = new DealColumnMapping(
            "owner_code", "client_code", "status", "close_date", "probability",
            "deal_value", "tentative_close_date", "stage", "product", "sector", "created_date");

    private ForecastService forecastService;

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
        ForecastProperties forecastProperties = new ForecastProperties(Map.of("HIGH", 0.8, "MEDIUM", 0.5, "LOW", 0.2));
        forecastService = new ForecastService(dealQueryService, forecastProperties);
    }

    @Test
    void forecast_endToEndThroughRealFetchNormalizeFilterChain() {
        wireMock.stubFor(post(urlEqualTo("/")).willReturn(okJson("""
                {"data":{"boards":[{"items_page":{"cursor":null,"items":[
                    {"id":"1","name":"Naruto","column_values":[
                        {"id":"status","text":"Open","value":null},
                        {"id":"probability","text":"High","value":null},
                        {"id":"deal_value","text":"100000","value":null},
                        {"id":"tentative_close_date","text":"2026-08-15","value":null}
                    ]},
                    {"id":"2","name":"Sasuke","column_values":[
                        {"id":"status","text":"Open","value":null},
                        {"id":"probability","text":"","value":null},
                        {"id":"deal_value","text":"50000","value":null},
                        {"id":"tentative_close_date","text":"2026-07-10","value":null}
                    ]},
                    {"id":"3","name":"Out Of Horizon","column_values":[
                        {"id":"status","text":"Open","value":null},
                        {"id":"probability","text":"High","value":null},
                        {"id":"deal_value","text":"999999","value":null},
                        {"id":"tentative_close_date","text":"2027-01-01","value":null}
                    ]}
                ]}}]}}
                """)));

        DateRange horizon = new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30), "this quarter");
        ForecastResponse response = forecastService.forecast(horizon, null);

        assertThat(response.bestCase()).isEqualByComparingTo("150000"); // Naruto + Sasuke, Out Of Horizon excluded
        assertThat(response.weightedForecast()).isEqualByComparingTo("80000"); // only Naruto: 100000 * 0.8
        assertThat(response.excludedDealCount()).isEqualTo(1); // Sasuke: blank probability
    }
}
