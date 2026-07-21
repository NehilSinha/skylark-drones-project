package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.analytics.CollectionsAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.CollectionsAnalyticsService;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionsAnalyticsToolTest {

    @Test
    void getCollectionsAnalytics_delegatesToServiceAndRecordsInvocation() {
        CollectionsAnalyticsService service = Mockito.mock(CollectionsAnalyticsService.class);
        ToolInvocationTracker tracker = new ToolInvocationTracker();
        CollectionsAnalyticsTool tool = new CollectionsAnalyticsTool(service, tracker);

        EntityFilter filter = EntityFilter.empty();
        CollectionsAnalyticsResponse expected = new CollectionsAnalyticsResponse(BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());
        when(service.snapshot(filter)).thenReturn(expected);

        CollectionsAnalyticsResponse actual = tool.getCollectionsAnalytics(filter);

        assertThat(actual).isSameAs(expected);
        verify(service).snapshot(filter);
        assertThat(tracker.drain()).containsExactly("CollectionsAnalyticsTool");
    }
}
