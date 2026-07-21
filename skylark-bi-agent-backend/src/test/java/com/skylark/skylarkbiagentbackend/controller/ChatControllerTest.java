package com.skylark.skylarkbiagentbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skylark.skylarkbiagentbackend.agent.tool.ToolInvocationTracker;
import com.skylark.skylarkbiagentbackend.dto.chat.ChatRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import com.skylark.skylarkbiagentbackend.config.WebConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies ChatController's own responsibilities — request validation, response
 * mapping, error translation — with {@link ChatClient} mocked. This deliberately
 * does NOT exercise Spring AI's internal tool-execution loop (that's framework
 * code); the walking skeleton's real tool-calling path is proven by the live smoke
 * test against the actual Groq API documented in the Phase 4 completion report.
 *
 * <p>{@code WebConfig} is excluded from this slice: {@code @WebMvcTest} pulls in
 * any {@code WebMvcConfigurer} bean regardless of the {@code controllers} filter
 * (since CORS/interceptor config directly affects MVC behavior), but WebConfig
 * depends on {@code AppProperties}, which this narrow slice has no reason to wire
 * just to satisfy an incidental dependency unrelated to what's under test here.
 */
@WebMvcTest(controllers = ChatController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class))
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Constructed directly rather than @Autowired: this slice's auto-configured
    // ObjectMapper bean isn't reliably present depending on which Jackson
    // autoconfiguration classes the slice pulls in, and ChatRequest is a plain
    // two-string record with no need for Spring's customized Jackson config.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @MockitoBean
    private ToolInvocationTracker toolInvocationTracker;

    @Test
    void chat_returnsAnswerAndToolsInvoked() throws Exception {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("Your Mining pipeline is worth 150,000.");
        when(toolInvocationTracker.drain()).thenReturn(List.of("PipelineAnalyticsTool"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest("sess-1", "What's our Mining pipeline?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-1"))
                .andExpect(jsonPath("$.answer").value("Your Mining pipeline is worth 150,000."))
                .andExpect(jsonPath("$.dataBacked").value(true))
                .andExpect(jsonPath("$.toolsInvoked[0]").value("PipelineAnalyticsTool"));
    }

    @Test
    void chat_noToolInvoked_isNotDataBacked() throws Exception {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("Could you tell me which sector you're asking about?");
        when(toolInvocationTracker.drain()).thenReturn(List.of());

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest("sess-1", "How's the pipeline?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataBacked").value(false))
                .andExpect(jsonPath("$.toolsInvoked").isEmpty());
    }

    @Test
    void chat_blankMessage_returns400WithValidationErrorCode() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest("sess-1", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void chat_blankSessionId_returns400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest("", "hello"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_chatClientFailure_mapsToServiceUnavailableViaLlmException() throws Exception {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenThrow(new RuntimeException("groq unreachable"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest("sess-1", "How's the pipeline?"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("LLM_ERROR"));
    }
}
