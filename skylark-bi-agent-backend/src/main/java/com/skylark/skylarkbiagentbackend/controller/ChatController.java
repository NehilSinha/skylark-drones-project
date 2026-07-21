package com.skylark.skylarkbiagentbackend.controller;

import com.skylark.skylarkbiagentbackend.agent.tool.ToolInvocationTracker;
import com.skylark.skylarkbiagentbackend.dto.chat.ChatRequest;
import com.skylark.skylarkbiagentbackend.dto.chat.ChatResponse;
import com.skylark.skylarkbiagentbackend.exception.LlmException;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The conversational endpoint. Conversation memory (Mongo-backed {@code
 * filterState}/history) is deferred to Phase 9 — this walking skeleton handles a
 * single turn per request; {@code sessionId} is accepted and echoed back but not
 * yet persisted. See PHASE-4-DESIGN.md Phase 9.
 *
 * <p>Note on error handling: a failure inside a tool call (e.g. {@code
 * MondayApiException} from {@code PipelineAnalyticsService}) is caught by Spring
 * AI's default tool-execution handling and turned into a tool-error string fed back
 * to the model, which then explains the failure conversationally — it does not
 * propagate to this controller as a Java exception. This is intentional for the
 * chat endpoint specifically: a graceful "I couldn't reach Monday.com right now" is
 * better UX mid-conversation than a raw 502. Direct REST endpoints without an LLM
 * in the loop (dashboard, reports) still surface these as proper HTTP errors via
 * {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final ToolInvocationTracker toolInvocationTracker;

    public ChatController(ChatClient chatClient, ToolInvocationTracker toolInvocationTracker) {
        this.chatClient = chatClient;
        this.toolInvocationTracker = toolInvocationTracker;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        try {
            String answer = chatClient.prompt()
                    .user(request.message())
                    .call()
                    .content();
            List<String> toolsInvoked = toolInvocationTracker.drain();
            return new ChatResponse(request.sessionId(), answer, !toolsInvoked.isEmpty(), toolsInvoked);
        } catch (RuntimeException e) {
            toolInvocationTracker.drain();
            throw new LlmException("Failed to get a response from the AI provider", e);
        }
    }
}
