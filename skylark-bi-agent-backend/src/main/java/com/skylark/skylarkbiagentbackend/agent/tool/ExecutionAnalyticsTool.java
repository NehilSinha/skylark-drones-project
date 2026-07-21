package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.analytics.ExecutionAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.ExecutionAnalyticsService;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ExecutionAnalyticsTool implements AgentTool {

    private final ExecutionAnalyticsService executionAnalyticsService;
    private final ToolInvocationTracker toolInvocationTracker;

    public ExecutionAnalyticsTool(ExecutionAnalyticsService executionAnalyticsService, ToolInvocationTracker toolInvocationTracker) {
        this.executionAnalyticsService = executionAnalyticsService;
        this.toolInvocationTracker = toolInvocationTracker;
    }

    @Tool(description = "Get work order execution status: a count of work orders per execution "
            + "status, a list of delayed work orders (overdue and not completed/recurring), and "
            + "the average variance in days between probable end date and actual data delivery date.")
    public ExecutionAnalyticsResponse getExecutionAnalytics(
            @ToolParam(required = false, description = "Optional filter: sectors, owners, clients, date range against probable end date.")
            EntityFilter filter) {
        toolInvocationTracker.record("ExecutionAnalyticsTool");
        return executionAnalyticsService.snapshot(filter);
    }
}
