package com.skylark.skylarkbiagentbackend.client.monday;

import com.skylark.skylarkbiagentbackend.client.monday.dto.BoardsItemsPageData;
import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayGraphQlRequest;
import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayGraphQlResponse;
import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayItemDto;
import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayItemsPageDto;
import com.skylark.skylarkbiagentbackend.client.monday.dto.NextItemsPageData;
import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.exception.MondayApiException;
import com.skylark.skylarkbiagentbackend.exception.MondayAuthenticationException;
import com.skylark.skylarkbiagentbackend.exception.MondayRateLimitException;
import com.skylark.skylarkbiagentbackend.exception.MondayTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single point of contact with Monday.com's GraphQL API v2. Every board-specific
 * service (deals, work orders, future boards) goes through this client rather than
 * building its own HTTP call — see ADR-001 "Monday API Interaction" for the full
 * design rationale (auth, pagination, retry, rate limiting, error mapping).
 */
@Component
public class MondayGraphQLClient {

    private static final Logger log = LoggerFactory.getLogger(MondayGraphQLClient.class);

    private final RestClient mondayRestClient;
    private final RetryTemplate mondayRetryTemplate;
    private final MondayRateLimiter rateLimiter;
    private final MondayQueryBuilder queryBuilder;
    private final MondayProperties properties;

    public MondayGraphQLClient(RestClient mondayRestClient,
                                RetryTemplate mondayRetryTemplate,
                                MondayRateLimiter rateLimiter,
                                MondayQueryBuilder queryBuilder,
                                MondayProperties properties) {
        this.mondayRestClient = mondayRestClient;
        this.mondayRetryTemplate = mondayRetryTemplate;
        this.rateLimiter = rateLimiter;
        this.queryBuilder = queryBuilder;
        this.properties = properties;
    }

    /**
     * Fetches every item on a board, transparently following Monday's cursor
     * pagination until exhausted or {@code monday.pagination.max-pages} is reached
     * (a safety cap against an API misbehaving and returning a cursor forever).
     */
    public List<MondayItemDto> fetchAllBoardItems(String boardId) {
        requireConfigured();
        int pageSize = properties.pagination().pageSize();
        int maxPages = properties.pagination().maxPages();

        List<MondayItemDto> allItems = new ArrayList<>();

        MondayItemsPageDto page = execute(
                queryBuilder.initialItemsPageQuery(),
                queryBuilder.initialItemsPageVariables(boardId, pageSize),
                new ParameterizedTypeReference<MondayGraphQlResponse<BoardsItemsPageData>>() {
                })
                .boards().stream().findFirst()
                .map(BoardsItemsPageData.BoardWrapper::itemsPage)
                .orElseThrow(() -> new MondayApiException("Board " + boardId + " was not found or returned no items_page"));

        allItems.addAll(page.items());
        int pagesFetched = 1;

        while (page.cursor() != null && pagesFetched < maxPages) {
            page = execute(
                    queryBuilder.nextItemsPageQuery(),
                    queryBuilder.nextItemsPageVariables(page.cursor(), pageSize),
                    new ParameterizedTypeReference<MondayGraphQlResponse<NextItemsPageData>>() {
                    })
                    .nextItemsPage();
            allItems.addAll(page.items());
            pagesFetched++;
        }

        if (page.cursor() != null) {
            log.warn("Board {} pagination stopped after the {}-page safety cap with more data remaining; "
                    + "raise monday.pagination.max-pages if this board is expected to be this large.", boardId, maxPages);
        }

        return allItems;
    }

    /** Escape hatch for board-specific queries (metadata, single-item lookups, etc.) beyond item pagination. */
    public <T> T executeQuery(String query, Map<String, Object> variables, ParameterizedTypeReference<MondayGraphQlResponse<T>> responseType) {
        requireConfigured();
        return execute(query, variables, responseType);
    }

    private <T> T execute(String query, Map<String, Object> variables, ParameterizedTypeReference<MondayGraphQlResponse<T>> responseType) {
        rateLimiter.acquire();

        RetryCallback<MondayGraphQlResponse<T>, RuntimeException> retryCallback =
                context -> callHttp(query, variables, responseType);

        // RetryTemplate invokes the recovery callback whenever the loop ends, whether
        // that's because retries were exhausted OR because the exception simply wasn't
        // retryable in the first place (e.g. an auth failure on attempt 1) — so any
        // exception that already has proper typing (thrown directly by callHttp) must
        // be re-thrown as-is here, not re-wrapped, or it loses its specific type and
        // the GlobalExceptionHandler's dedicated branch for it (e.g. 401/403) never fires.
        RecoveryCallback<MondayGraphQlResponse<T>> recoveryCallback = context -> {
            Throwable last = context.getLastThrowable();
            if (last instanceof TransientMondayFailureException transientFailure) {
                if (transientFailure.isRateLimit()) {
                    throw new MondayRateLimitException(transientFailure.getMessage());
                }
                throw new MondayTimeoutException(transientFailure.getMessage(),
                        transientFailure.getCause() != null ? transientFailure.getCause() : transientFailure);
            }
            if (last instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new MondayApiException("Monday.com request failed", last);
        };

        MondayGraphQlResponse<T> response = mondayRetryTemplate.execute(retryCallback, recoveryCallback);

        if (response.hasErrors()) {
            String combined = response.errors().stream()
                    .map(e -> e.message())
                    .collect(Collectors.joining("; "));
            throw new MondayApiException("Monday.com returned GraphQL errors: " + combined);
        }
        return response.data();
    }

    private <T> MondayGraphQlResponse<T> callHttp(String query, Map<String, Object> variables,
                                                    ParameterizedTypeReference<MondayGraphQlResponse<T>> responseType) {
        try {
            return mondayRestClient.post()
                    .body(new MondayGraphQlRequest(query, variables))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        int code = res.getStatusCode().value();
                        if (code == 401 || code == 403) {
                            throw new MondayAuthenticationException(
                                    "Monday.com rejected the configured API token (HTTP " + code + ")");
                        }
                        if (code == 429) {
                            throw new TransientMondayFailureException("Monday.com rate limit (HTTP 429)", true, null);
                        }
                        if (code >= 500) {
                            throw new TransientMondayFailureException("Monday.com server error (HTTP " + code + ")", false, null);
                        }
                        throw new MondayApiException("Monday.com returned HTTP " + code);
                    })
                    .body(responseType);
        } catch (ResourceAccessException e) {
            // Covers both connect and read timeouts, plus general connectivity failures.
            throw new TransientMondayFailureException("Network error calling Monday.com", false, e);
        }
    }

    private void requireConfigured() {
        if (!properties.api().isConfigured()) {
            throw new MondayApiException(
                    "Monday.com integration is not configured — set MONDAY_API_TOKEN before making requests.");
        }
    }
}
