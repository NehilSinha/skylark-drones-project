package com.skylark.skylarkbiagentbackend.controller;

import com.skylark.skylarkbiagentbackend.analytics.BillingAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.CollectionsAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.ExecutionAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.ForecastResponse;
import com.skylark.skylarkbiagentbackend.analytics.ForecastService;
import com.skylark.skylarkbiagentbackend.analytics.PipelineAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.PipelineAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.RevenueAnalyticsService;
import com.skylark.skylarkbiagentbackend.config.WebConfig;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class))
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PipelineAnalyticsService pipelineAnalyticsService;
    @MockitoBean
    private ForecastService forecastService;
    @MockitoBean
    private RevenueAnalyticsService revenueAnalyticsService;
    @MockitoBean
    private ExecutionAnalyticsService executionAnalyticsService;
    @MockitoBean
    private BillingAnalyticsService billingAnalyticsService;
    @MockitoBean
    private CollectionsAnalyticsService collectionsAnalyticsService;

    @Test
    void pipeline_noFilter_returns200() throws Exception {
        PipelineAnalyticsResponse response = new PipelineAnalyticsResponse(
                BigDecimal.TEN, BigDecimal.ONE, Map.of(), BigDecimal.ONE, 0.5, List.of(), List.of());
        when(pipelineAnalyticsService.snapshot(null)).thenReturn(response);

        mockMvc.perform(get("/api/analytics/pipeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPipelineValue").value(10));
    }

    @Test
    void pipeline_withSectorFilter_buildsEntityFilterFromQueryParams() throws Exception {
        PipelineAnalyticsResponse response = new PipelineAnalyticsResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), BigDecimal.ZERO, 0.0, List.of(), List.of());
        when(pipelineAnalyticsService.snapshot(any())).thenReturn(response);

        mockMvc.perform(get("/api/analytics/pipeline").param("sector", "Mining"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(pipelineAnalyticsService)
                .snapshot(eq(new EntityFilter(List.of("Mining"), null, null, null, null)));
    }

    @Test
    void forecast_requiresHorizonDates() throws Exception {
        mockMvc.perform(get("/api/analytics/forecast"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forecast_withHorizon_returns200() throws Exception {
        ForecastResponse response = new ForecastResponse(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 0, List.of());
        when(forecastService.forecast(any(DateRange.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/analytics/forecast")
                        .param("horizonStart", "2026-07-01")
                        .param("horizonEnd", "2026-09-30"))
                .andExpect(status().isOk());
    }

    @Test
    void pipeline_onlyOneOfDateFromDateTo_returns400() throws Exception {
        mockMvc.perform(get("/api/analytics/pipeline").param("dateFrom", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
