package com.skylark.skylarkbiagentbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI skylarkBiAgentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Skylark AI Business Intelligence Agent")
                .description("Conversational BI over Monday.com deal and work-order data. "
                        + "Monday.com is the business system of record; MongoDB backs only "
                        + "conversation memory, reports, and caching.")
                .version("v1")
                .contact(new Contact().name("Skylark Engineering")));
    }
}
