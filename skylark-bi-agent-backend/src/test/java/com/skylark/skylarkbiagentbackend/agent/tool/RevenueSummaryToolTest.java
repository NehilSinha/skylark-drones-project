package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.analytics.RevenueAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.RevenueSummaryResponse;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevenueSummaryToolTest {

    @Test
    void getRevenueSummary_delegatesToServiceAndRecordsInvocation() {
        RevenueAnalyticsService service = Mockito.mock(RevenueAnalyticsService.class);
        ToolInvocationTracker tracker = new ToolInvocationTracker();
        RevenueSummaryTool tool = new RevenueSummaryTool(service, tracker);

        EntityFilter filter = EntityFilter.empty();
        RevenueSummaryResponse expected = new RevenueSummaryResponse(BigDecimal.TEN, null, null, null, List.of());
        when(service.summary(filter)).thenReturn(expected);

        RevenueSummaryResponse actual = tool.getRevenueSummary(filter);

        assertThat(actual).isSameAs(expected);
        verify(service).summary(filter);
        assertThat(tracker.drain()).containsExactly("RevenueSummaryTool");
    }
}
