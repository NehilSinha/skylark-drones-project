package com.skylark.skylarkbiagentbackend.controller;

import com.skylark.skylarkbiagentbackend.analytics.BillingAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.BillingAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.CollectionsAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.CollectionsAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.ExecutionAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.ExecutionAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.ForecastResponse;
import com.skylark.skylarkbiagentbackend.analytics.ForecastService;
import com.skylark.skylarkbiagentbackend.analytics.PipelineAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.PipelineAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.RevenueAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.RevenueSummaryResponse;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.exception.ValidationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Direct, deterministic REST access to every analytics service built in Phase 5
 * batches 1–2 — no LLM in the loop, so these never depend on Groq availability and
 * are what the dashboard/analytics pages call directly. The chat endpoint
 * ({@link ChatController}) reaches the same services through their {@code @Tool}
 * wrappers for conversational access; this controller is the programmatic path.
 *
 * <p>Filters are query parameters, not a JSON body — a REST client (the frontend)
 * supplies concrete values (a date picker, a multi-select), unlike the chat path
 * where a phrase needs LLM-mediated resolution. {@code forecast} therefore takes
 * explicit {@code horizonStart}/{@code horizonEnd} dates rather than a phrase.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final PipelineAnalyticsService pipelineAnalyticsService;
    private final ForecastService forecastService;
    private final RevenueAnalyticsService revenueAnalyticsService;
    private final ExecutionAnalyticsService executionAnalyticsService;
    private final BillingAnalyticsService billingAnalyticsService;
    private final CollectionsAnalyticsService collectionsAnalyticsService;

    public AnalyticsController(PipelineAnalyticsService pipelineAnalyticsService,
                                ForecastService forecastService,
                                RevenueAnalyticsService revenueAnalyticsService,
                                ExecutionAnalyticsService executionAnalyticsService,
                                BillingAnalyticsService billingAnalyticsService,
                                CollectionsAnalyticsService collectionsAnalyticsService) {
        this.pipelineAnalyticsService = pipelineAnalyticsService;
        this.forecastService = forecastService;
        this.revenueAnalyticsService = revenueAnalyticsService;
        this.executionAnalyticsService = executionAnalyticsService;
        this.billingAnalyticsService = billingAnalyticsService;
        this.collectionsAnalyticsService = collectionsAnalyticsService;
    }

    @GetMapping("/pipeline")
    public PipelineAnalyticsResponse pipeline(
            @RequestParam(required = false) List<String> sector,
            @RequestParam(required = false) List<String> owner,
            @RequestParam(required = false) List<String> client,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return pipelineAnalyticsService.snapshot(buildFilter(sector, owner, client, status, dateFrom, dateTo));
    }

    @GetMapping("/forecast")
    public ForecastResponse forecast(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate horizonStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate horizonEnd,
            @RequestParam(required = false) List<String> sector,
            @RequestParam(required = false) List<String> owner,
            @RequestParam(required = false) List<String> client) {
        DateRange horizon = new DateRange(horizonStart, horizonEnd, "custom");
        return forecastService.forecast(horizon, buildFilter(sector, owner, client, null, null, null));
    }

    @GetMapping("/revenue")
    public RevenueSummaryResponse revenue(
            @RequestParam(required = false) List<String> sector,
            @RequestParam(required = false) List<String> owner,
            @RequestParam(required = false) List<String> client,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return revenueAnalyticsService.summary(buildFilter(sector, owner, client, null, dateFrom, dateTo));
    }

    @GetMapping("/execution")
    public ExecutionAnalyticsResponse execution(
            @RequestParam(required = false) List<String> sector,
            @RequestParam(required = false) List<String> owner,
            @RequestParam(required = false) List<String> client,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return executionAnalyticsService.snapshot(buildFilter(sector, owner, client, null, dateFrom, dateTo));
    }

    @GetMapping("/billing")
    public BillingAnalyticsResponse billing(
            @RequestParam(required = false) List<String> sector,
            @RequestParam(required = false) List<String> owner,
            @RequestParam(required = false) List<String> client,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return billingAnalyticsService.snapshot(buildFilter(sector, owner, client, null, dateFrom, dateTo));
    }

    @GetMapping("/collections")
    public CollectionsAnalyticsResponse collections(
            @RequestParam(required = false) List<String> sector,
            @RequestParam(required = false) List<String> owner,
            @RequestParam(required = false) List<String> client,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return collectionsAnalyticsService.snapshot(buildFilter(sector, owner, client, null, dateFrom, dateTo));
    }

    private EntityFilter buildFilter(List<String> sector, List<String> owner, List<String> client,
                                      List<String> status, LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null ^ dateTo != null) {
            throw new ValidationException("dateFrom and dateTo must both be provided together");
        }
        DateRange dateRange = dateFrom != null ? new DateRange(dateFrom, dateTo, "custom") : null;
        if (sector == null && owner == null && client == null && status == null && dateRange == null) {
            return null;
        }
        return new EntityFilter(sector, owner, client, status, dateRange);
    }
}
