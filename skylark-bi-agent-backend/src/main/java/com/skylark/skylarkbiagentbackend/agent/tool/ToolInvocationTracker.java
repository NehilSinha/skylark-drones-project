package com.skylark.skylarkbiagentbackend.agent.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Records which {@code @Tool}-annotated beans were invoked during the current
 * request, so {@code ChatController} can report {@code toolsInvoked}/{@code
 * dataBacked} without depending on Spring AI's internal tool-execution API (which
 * doesn't expose a stable, simple way to inspect the intermediate tool-call trace —
 * see PHASE-4-DESIGN.md walking-skeleton notes).
 *
 * <p>Safe under Spring MVC's one-thread-per-request model: each tool method calls
 * {@link #record(String)} on the same thread handling the HTTP request, and the
 * controller must call {@link #drain()} exactly once per request (in a
 * {@code finally} block) so a pooled thread never starts its next request with a
 * stale list.
 */
@Component
public class ToolInvocationTracker {

    private final ThreadLocal<List<String>> invoked = ThreadLocal.withInitial(ArrayList::new);

    public void record(String toolName) {
        invoked.get().add(toolName);
    }

    public List<String> drain() {
        List<String> result = List.copyOf(invoked.get());
        invoked.remove();
        return result;
    }
}
