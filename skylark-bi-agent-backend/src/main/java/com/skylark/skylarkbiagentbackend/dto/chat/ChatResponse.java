package com.skylark.skylarkbiagentbackend.dto.chat;

import java.util.List;

/**
 * Walking-skeleton shape. {@code warnings}/{@code charts} from PHASE-4-DESIGN.md's
 * full {@code ChatResponse} are deferred — extracting a tool's
 * {@code List<DataQualityWarning>} back out to this level needs either a
 * structured-output pass or an advisor around tool execution, neither of which is
 * built yet. {@code toolsInvoked}/{@code dataBacked} are populated via
 * {@code ToolInvocationTracker} instead of Spring AI's tool-call metadata — see
 * that class's Javadoc for why.
 */
public record ChatResponse(
        String sessionId,
        String answer,
        boolean dataBacked,
        List<String> toolsInvoked
) {
}
