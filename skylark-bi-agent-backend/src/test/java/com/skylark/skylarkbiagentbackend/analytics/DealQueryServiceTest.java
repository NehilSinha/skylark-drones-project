package com.skylark.skylarkbiagentbackend.analytics;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.skylark.skylarkbiagentbackend.client.monday.MondayGraphQLClient;
import com.skylark.skylarkbiagentbackend.client.monday.MondayQueryBuilder;
import com.skylark.skylarkbiagentbackend.client.monday.MondayRateLimiter;
import com.skylark.skylarkbiagentbackend.config.DealColumnMapping;
import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.normalizer.DealNormalizer;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedDeal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the fetch -> filter-stray-rows -> normalize -> filter-by-EntityFilter
 * pipeline end to end against a real HTTP server, using the exact column-ID mapping
 * the walking skeleton's live smoke test also uses.
 */
class DealQueryServiceTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    private static final DealColumnMapping MAPPING = new DealColumnMapping(
            "owner_code", "client_code", "status", "close_date", "probability",
            "deal_value", "tentative_close_date", "stage", "product", "sector", "created_date");

    private DealQueryService dealQueryService;

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

        dealQueryService = new DealQueryService(client, properties, new DealNormalizer(), MAPPING);
    }

    @Test
    void queryAll_normalizesAndFiltersStrayHeaderRows() {
        wireMock.stubFor(post(urlEqualTo("/")).willReturn(okJson("""
                {"data":{"boards":[{"items_page":{"cursor":null,"items":[
                    {"id":"1","name":"Naruto","column_values":[
                        {"id":"status","text":"Open","value":null},
                        {"id":"sector","text":"Mining","value":null},
                        {"id":"deal_value","text":"489360","value":null}
                    ]},
                    {"id":"2","name":"Deal Status","column_values":[]}
                ]}}]}}
                """)));

        List<NormalizedDeal> deals = dealQueryService.queryAll(null);

        assertThat(deals).hasSize(1);
        assertThat(deals.get(0).dealName()).isEqualTo("Naruto");
    }

    @Test
    void queryAll_appliesSectorFilter() {
        wireMock.stubFor(post(urlEqualTo("/")).willReturn(okJson("""
                {"data":{"boards":[{"items_page":{"cursor":null,"items":[
                    {"id":"1","name":"Mining Deal","column_values":[{"id":"sector","text":"Mining","value":null}]},
                    {"id":"2","name":"Renewables Deal","column_values":[{"id":"sector","text":"Renewables","value":null}]}
                ]}}]}}
                """)));

        EntityFilter filter = new EntityFilter(List.of("Mining"), null, null, null, null);
        List<NormalizedDeal> deals = dealQueryService.queryAll(filter);

        assertThat(deals).extracting(NormalizedDeal::dealName).containsExactly("Mining Deal");
    }

    @Test
    void queryAll_dateRangeFilter_excludesDealsWithNoTentativeCloseDate() {
        wireMock.stubFor(post(urlEqualTo("/")).willReturn(okJson("""
                {"data":{"boards":[{"items_page":{"cursor":null,"items":[
                    {"id":"1","name":"In Range","column_values":[{"id":"tentative_close_date","text":"2026-08-15","value":null}]},
                    {"id":"2","name":"Out Of Range","column_values":[{"id":"tentative_close_date","text":"2025-01-01","value":null}]},
                    {"id":"3","name":"No Date","column_values":[]}
                ]}}]}}
                """)));

        EntityFilter filter = new EntityFilter(null, null, null, null,
                new DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "this month"));
        List<NormalizedDeal> deals = dealQueryService.queryAll(filter);

        assertThat(deals).extracting(NormalizedDeal::dealName).containsExactly("In Range");
    }
}
