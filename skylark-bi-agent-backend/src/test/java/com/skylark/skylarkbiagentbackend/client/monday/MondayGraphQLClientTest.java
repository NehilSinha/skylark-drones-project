package com.skylark.skylarkbiagentbackend.client.monday;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayItemDto;
import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.exception.MondayApiException;
import com.skylark.skylarkbiagentbackend.exception.MondayAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link MondayGraphQLClient} against a real HTTP server (WireMock) rather
 * than mocking {@link RestClient}, so pagination, retry, and error-mapping behavior
 * is verified end-to-end through the actual HTTP stack.
 */
class MondayGraphQLClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    private MondayGraphQLClient client;

    @BeforeEach
    void setUp() {
        MondayProperties properties = new MondayProperties(
                new MondayProperties.Api(wireMock.baseUrl(), "test-token", 2000, 2000),
                new MondayProperties.Boards("1", "2"),
                new MondayProperties.Pagination(100, 10),
                new MondayProperties.RateLimit(1000),
                new MondayProperties.Retry(3, 10, 1.0)
        );

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.api().baseUrl())
                .defaultHeader("Authorization", properties.api().token())
                .build();

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3, Map.of(TransientMondayFailureException.class, true)));
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(5L);
        retryTemplate.setBackOffPolicy(backOff);

        client = new MondayGraphQLClient(restClient, retryTemplate, new MondayRateLimiter(properties), new MondayQueryBuilder(), properties);
    }

    @Test
    void fetchAllBoardItems_followsCursorPaginationAcrossPages() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("GetBoardItems"))
                .willReturn(okJson("""
                        {"data":{"boards":[{"items_page":{"cursor":"abc","items":[{"id":"1","name":"Naruto","column_values":[]}]}}]}}
                        """)));

        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("GetNextItems"))
                .willReturn(okJson("""
                        {"data":{"next_items_page":{"cursor":null,"items":[{"id":"2","name":"Sasuke","column_values":[]}]}}}
                        """)));

        List<MondayItemDto> items = client.fetchAllBoardItems("1");

        assertThat(items).extracting(MondayItemDto::name).containsExactly("Naruto", "Sasuke");
    }

    @Test
    void fetchAllBoardItems_mapsHttp401ToAuthenticationExceptionWithoutRetrying() {
        wireMock.stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(() -> client.fetchAllBoardItems("1"))
                .isInstanceOf(MondayAuthenticationException.class);
    }

    @Test
    void fetchAllBoardItems_retriesTransientServerErrorThenSucceeds() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("GetBoardItems"))
                .inScenario("retry-recovery")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("recovered"));

        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("GetBoardItems"))
                .inScenario("retry-recovery")
                .whenScenarioStateIs("recovered")
                .willReturn(okJson("""
                        {"data":{"boards":[{"items_page":{"cursor":null,"items":[{"id":"1","name":"Naruto","column_values":[]}]}}]}}
                        """)));

        List<MondayItemDto> items = client.fetchAllBoardItems("1");

        assertThat(items).hasSize(1);
    }

    @Test
    void fetchAllBoardItems_mapsGraphQlErrorsArrayEvenOnHttp200() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                        {"data":null,"errors":[{"message":"board not found"}]}
                        """)));

        assertThatThrownBy(() -> client.fetchAllBoardItems("1"))
                .isInstanceOf(MondayApiException.class)
                .hasMessageContaining("board not found");
    }
}
