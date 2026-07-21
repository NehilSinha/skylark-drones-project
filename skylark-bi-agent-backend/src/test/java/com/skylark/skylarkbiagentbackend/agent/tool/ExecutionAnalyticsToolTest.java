package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.analytics.ExecutionAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.ExecutionAnalyticsService;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutionAnalyticsToolTest {

    @Test
    void getExecutionAnalytics_delegatesToServiceAndRecordsInvocation() {
        ExecutionAnalyticsService service = Mockito.mock(ExecutionAnalyticsService.class);
        ToolInvocationTracker tracker = new ToolInvocationTracker();
        ExecutionAnalyticsTool tool = new ExecutionAnalyticsTool(service, tracker);

        EntityFilter filter = EntityFilter.empty();
        ExecutionAnalyticsResponse expected = new ExecutionAnalyticsResponse(Map.of(), List.of(), null, List.of());
        when(service.snapshot(filter)).thenReturn(expected);

        ExecutionAnalyticsResponse actual = tool.getExecutionAnalytics(filter);

        assertThat(actual).isSameAs(expected);
        verify(service).snapshot(filter);
        assertThat(tracker.drain()).containsExactly("ExecutionAnalyticsTool");
    }
}
