package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.analytics.PipelineAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.PipelineAnalyticsService;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineAnalyticsToolTest {

    @Test
    void getPipelineAnalytics_delegatesToServiceAndRecordsInvocation() {
        PipelineAnalyticsService service = Mockito.mock(PipelineAnalyticsService.class);
        ToolInvocationTracker tracker = new ToolInvocationTracker();
        PipelineAnalyticsTool tool = new PipelineAnalyticsTool(service, tracker);

        EntityFilter filter = new EntityFilter(List.of("Mining"), null, null, null, null);
        PipelineAnalyticsResponse expected = new PipelineAnalyticsResponse(
                BigDecimal.TEN, BigDecimal.ONE, Map.of(), BigDecimal.ONE, 0.5, List.of(), List.of());
        when(service.snapshot(filter)).thenReturn(expected);

        PipelineAnalyticsResponse actual = tool.getPipelineAnalytics(filter);

        assertThat(actual).isSameAs(expected);
        verify(service).snapshot(filter);
        assertThat(tracker.drain()).containsExactly("PipelineAnalyticsTool");
    }
}
