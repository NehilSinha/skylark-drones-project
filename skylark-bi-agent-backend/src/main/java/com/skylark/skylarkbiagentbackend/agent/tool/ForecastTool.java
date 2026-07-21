package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.agent.router.DateExpressionResolver;
import com.skylark.skylarkbiagentbackend.analytics.ForecastResponse;
import com.skylark.skylarkbiagentbackend.analytics.ForecastService;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Resolves its own horizon phrase internally via {@link DateExpressionResolver}
 * rather than requiring the model to first call {@code resolveDateExpression} and
 * pass the result in — see {@code docs/PHASE-5-BATCH-1-NOTES.md} for why: live
 * testing against Groq's {@code llama-3.3-70b-versatile} showed it cannot reliably
 * chain "call tool
 * A, feed A's output into tool B's parameter." It would attempt to nest a raw
 * function-call structure inside the {@code horizon} argument instead of making two
 * sequential tool calls, which Groq's API-side schema validation then rejected
 * outright (a 400, before either tool ever ran). Taking a phrase directly and
 * resolving it in Java sidesteps the model's multi-step tool-planning reliability
 * entirely — the safer default for any future tool that needs a date, not just this
 * one.
 */
@Component
public class ForecastTool implements AgentTool {

    private final ForecastService forecastService;
    private final DateExpressionResolver dateExpressionResolver;
    private final ToolInvocationTracker toolInvocationTracker;

    public ForecastTool(ForecastService forecastService,
                         DateExpressionResolver dateExpressionResolver,
                         ToolInvocationTracker toolInvocationTracker) {
        this.forecastService = forecastService;
        this.dateExpressionResolver = dateExpressionResolver;
        this.toolInvocationTracker = toolInvocationTracker;
    }

    @Tool(description = "Get a probability-weighted revenue forecast for open deals expected to "
            + "close within a horizon: the weighted forecast value, an optimistic best case (every "
            + "eligible deal closes), a conservative worst case (only high-confidence deals close), "
            + "and how many deals were excluded from weighting for missing closure probability.")
    public ForecastResponse getForecast(
            @ToolParam(description = "The forecast horizon as a natural-language phrase, e.g. "
                    + "\"this quarter\", \"next 30 days\", \"this year\" — pass the phrase directly, "
                    + "it is resolved internally.")
            String horizonPhrase,
            @ToolParam(required = false, description = "Optional filter: sectors, owners, clients. "
                    + "dealStatuses/dateRange are ignored here since the forecast is inherently "
                    + "scoped to open deals within the horizon.")
            EntityFilter filter) {
        toolInvocationTracker.record("ForecastTool");
        DateRange horizon = dateExpressionResolver.resolve(horizonPhrase);
        return forecastService.forecast(horizon, filter);
    }
}
