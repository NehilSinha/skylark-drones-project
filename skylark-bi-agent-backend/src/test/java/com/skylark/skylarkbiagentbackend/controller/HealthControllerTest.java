package com.skylark.skylarkbiagentbackend.controller;

import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void health_reportsConfigurationState() {
        MondayProperties properties = new MondayProperties(
                new MondayProperties.Api("http://localhost", "token", 1000, 1000),
                new MondayProperties.Boards("1", "2"),
                new MondayProperties.Pagination(100, 10),
                new MondayProperties.RateLimit(60),
                new MondayProperties.Retry(3, 500, 2.0));

        HealthController controller = new HealthController(properties, "groq-key");

        var response = controller.health();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.mondayApiConfigured()).isTrue();
        assertThat(response.mondayBoardsConfigured()).isTrue();
        assertThat(response.llmConfigured()).isTrue();
    }

    @Test
    void health_blankLlmKey_reportsNotConfigured() {
        MondayProperties properties = new MondayProperties(
                new MondayProperties.Api("http://localhost", "", 1000, 1000),
                new MondayProperties.Boards("", ""),
                new MondayProperties.Pagination(100, 10),
                new MondayProperties.RateLimit(60),
                new MondayProperties.Retry(3, 500, 2.0));

        HealthController controller = new HealthController(properties, "");

        var response = controller.health();

        assertThat(response.mondayApiConfigured()).isFalse();
        assertThat(response.mondayBoardsConfigured()).isFalse();
        assertThat(response.llmConfigured()).isFalse();
    }
}
