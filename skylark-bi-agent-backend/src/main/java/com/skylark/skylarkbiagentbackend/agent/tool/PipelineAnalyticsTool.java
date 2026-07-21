package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.analytics.PipelineAnalyticsResponse;
import com.skylark.skylarkbiagentbackend.analytics.PipelineAnalyticsService;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Agent-facing wrapper around {@link PipelineAnalyticsService}. Thin by design —
 * all business logic lives in the service; this class only translates the LLM's
 * tool call into a service invocation and records that it ran (see
 * {@link ToolInvocationTracker}).
 */
@Component
public class PipelineAnalyticsTool implements AgentTool {

    private final PipelineAnalyticsService pipelineAnalyticsService;
    private final ToolInvocationTracker toolInvocationTracker;

    public PipelineAnalyticsTool(PipelineAnalyticsService pipelineAnalyticsService, ToolInvocationTracker toolInvocationTracker) {
        this.pipelineAnalyticsService = pipelineAnalyticsService;
        this.toolInvocationTracker = toolInvocationTracker;
    }

    @Tool(description = "Get a snapshot of the deal pipeline: total pipeline value, "
            + "a probability-weighted forecast value, the stage funnel (deal count and "
            + "value per Deal Stage), average deal size, win rate, and aging buckets for "
            + "open deals. Optionally scoped by sector, owner, client, deal status, and/or "
            + "a resolved date range matched against each deal's tentative close date.")
    public PipelineAnalyticsResponse getPipelineAnalytics(
            @ToolParam(required = false, description = "Optional filter: sectors, owners, "
                    + "clients, deal statuses, and/or a date range. Omit or leave fields empty "
                    + "for no filter on that dimension.")
            EntityFilter filter) {
        toolInvocationTracker.record("PipelineAnalyticsTool");
        return pipelineAnalyticsService.snapshot(filter);
    }
}
