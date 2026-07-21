package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.agent.router.DateExpressionResolver;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Agent-facing wrapper around {@link DateExpressionResolver} — this is how every
 * other tool that needs a date range gets one, since the model itself never
 * computes dates (see ADR-001 "AI Agent Workflow" and the system prompt in
 * {@code AiConfig}).
 */
@Component
public class ResolveDateExpressionTool implements AgentTool {

    private final DateExpressionResolver dateExpressionResolver;
    private final ToolInvocationTracker toolInvocationTracker;

    public ResolveDateExpressionTool(DateExpressionResolver dateExpressionResolver, ToolInvocationTracker toolInvocationTracker) {
        this.dateExpressionResolver = dateExpressionResolver;
        this.toolInvocationTracker = toolInvocationTracker;
    }

    @Tool(description = "Resolve a natural-language date phrase into a concrete start/end date "
            + "range. Call this BEFORE any other tool whenever the user's question uses a relative "
            + "phrase (e.g. \"this quarter\", \"last month\", \"next 30 days\", \"YTD\", \"overdue\") "
            + "rather than explicit dates, then pass the returned range into that tool. "
            + "Note: \"this quarter\"/\"this year\"/\"YTD\" use the Indian fiscal year (April-March).")
    public DateRange resolveDateExpression(
            @ToolParam(description = "The date phrase to resolve, e.g. \"this quarter\".") String phrase) {
        toolInvocationTracker.record("ResolveDateExpressionTool");
        return dateExpressionResolver.resolve(phrase);
    }
}
