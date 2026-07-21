package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.analytics.CollectionsAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.CollectionsAnalyticsService;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CollectionsAnalyticsTool implements AgentTool {

    private final CollectionsAnalyticsService collectionsAnalyticsService;
    private final ToolInvocationTracker toolInvocationTracker;

    public CollectionsAnalyticsTool(CollectionsAnalyticsService collectionsAnalyticsService, ToolInvocationTracker toolInvocationTracker) {
        this.collectionsAnalyticsService = collectionsAnalyticsService;
        this.toolInvocationTracker = toolInvocationTracker;
    }

    @Tool(description = "Get total collected revenue, total outstanding receivable, and a list of "
            + "aged receivables (oldest/largest outstanding amounts first) for work orders.")
    public CollectionsAnalyticsResponse getCollectionsAnalytics(
            @ToolParam(required = false, description = "Optional filter: sectors, owners, clients, date range against probable end date.")
            EntityFilter filter) {
        toolInvocationTracker.record("CollectionsAnalyticsTool");
        return collectionsAnalyticsService.snapshot(filter);
    }
}
