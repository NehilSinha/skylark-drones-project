package com.skylark.skylarkbiagentbackend.agent.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolInvocationTrackerTest {

    private final ToolInvocationTracker tracker = new ToolInvocationTracker();

    @Test
    void drain_returnsRecordedToolsInOrder() {
        tracker.record("ResolveDateExpressionTool");
        tracker.record("PipelineAnalyticsTool");

        assertThat(tracker.drain()).containsExactly("ResolveDateExpressionTool", "PipelineAnalyticsTool");
    }

    @Test
    void drain_clearsStateForTheNextRequestOnTheSameThread() {
        tracker.record("PipelineAnalyticsTool");
        tracker.drain();

        assertThat(tracker.drain()).isEmpty();
    }

    @Test
    void drain_withNoInvocations_isEmptyNotNull() {
        assertThat(tracker.drain()).isEmpty();
    }
}
