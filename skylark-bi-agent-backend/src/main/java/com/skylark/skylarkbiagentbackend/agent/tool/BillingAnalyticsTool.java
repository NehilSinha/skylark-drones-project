package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.analytics.BillingAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.BillingAnalyticsService;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class BillingAnalyticsTool implements AgentTool {

    private final BillingAnalyticsService billingAnalyticsService;
    private final ToolInvocationTracker toolInvocationTracker;

    public BillingAnalyticsTool(BillingAnalyticsService billingAnalyticsService, ToolInvocationTracker toolInvocationTracker) {
        this.billingAnalyticsService = billingAnalyticsService;
        this.toolInvocationTracker = toolInvocationTracker;
    }

    @Tool(description = "Get billing and invoice status breakdown for work orders (counts per "
            + "status) and the total amount still to be billed.")
    public BillingAnalyticsResponse getBillingAnalytics(
            @ToolParam(required = false, description = "Optional filter: sectors, owners, clients, date range against probable end date.")
            EntityFilter filter) {
        toolInvocationTracker.record("BillingAnalyticsTool");
        return billingAnalyticsService.snapshot(filter);
    }
}
