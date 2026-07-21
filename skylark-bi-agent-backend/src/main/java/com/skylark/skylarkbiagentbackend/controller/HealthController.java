package com.skylark.skylarkbiagentbackend.controller;

import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final MondayProperties mondayProperties;
    private final String llmApiKey;

    public HealthController(MondayProperties mondayProperties,
                             @Value("${spring.ai.openai.api-key:}") String llmApiKey) {
        this.mondayProperties = mondayProperties;
        this.llmApiKey = llmApiKey;
    }

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse(
                "UP",
                Instant.now(),
                mondayProperties.api().isConfigured(),
                mondayProperties.boards().isConfigured(),
                llmApiKey != null && !llmApiKey.isBlank());
    }
}
