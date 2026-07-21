package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.analytics.BillingAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.BillingAnalyticsService;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingAnalyticsToolTest {

    @Test
    void getBillingAnalytics_delegatesToServiceAndRecordsInvocation() {
        BillingAnalyticsService service = Mockito.mock(BillingAnalyticsService.class);
        ToolInvocationTracker tracker = new ToolInvocationTracker();
        BillingAnalyticsTool tool = new BillingAnalyticsTool(service, tracker);

        EntityFilter filter = EntityFilter.empty();
        BillingAnalyticsResponse expected = new BillingAnalyticsResponse(Map.of(), Map.of(), BigDecimal.ZERO, List.of());
        when(service.snapshot(filter)).thenReturn(expected);

        BillingAnalyticsResponse actual = tool.getBillingAnalytics(filter);

        assertThat(actual).isSameAs(expected);
        verify(service).snapshot(filter);
        assertThat(tracker.drain()).containsExactly("BillingAnalyticsTool");
    }
}
