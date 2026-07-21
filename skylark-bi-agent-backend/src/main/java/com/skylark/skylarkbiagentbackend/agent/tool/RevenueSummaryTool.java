package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.analytics.RevenueAnalyticsService;
import com.skylark.skylarkbiagentbackend.analytics.RevenueSummaryResponse;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class RevenueSummaryTool implements AgentTool {

    private final RevenueAnalyticsService revenueAnalyticsService;
    private final ToolInvocationTracker toolInvocationTracker;

    public RevenueSummaryTool(RevenueAnalyticsService revenueAnalyticsService, ToolInvocationTracker toolInvocationTracker) {
        this.revenueAnalyticsService = revenueAnalyticsService;
        this.toolInvocationTracker = toolInvocationTracker;
    }

    @Tool(description = "Get booked revenue (value of Won deals). Billed revenue, collected "
            + "revenue, and outstanding receivables are not available yet (Work Order board "
            + "integration is not built) and will always be null with a warning explaining why — "
            + "do not treat that null as zero.")
    public RevenueSummaryResponse getRevenueSummary(
            @ToolParam(required = false, description = "Optional filter: sectors, owners, clients, "
                    + "and/or a date range matched against each deal's actual close date. "
                    + "dealStatuses is ignored since booked revenue is always Won-scoped.")
            EntityFilter filter) {
        toolInvocationTracker.record("RevenueSummaryTool");
        return revenueAnalyticsService.summary(filter);
    }
}
