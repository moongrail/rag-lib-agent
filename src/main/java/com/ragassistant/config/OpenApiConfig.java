package com.ragassistant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ragAssistantOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("RAG Research Assistant API")
                .description("RAG-based assistant: upload documents (PDF, DOCX, TXT, MD, XML) and chat over them. " +
                        "Multi-tenant, hybrid retrieval, pluggable embedding/chat providers.")
                .version("0.1.0"));
    }
}
