package com.ragassistant.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void openAPI_beanHasTitle() {
        OpenAPI api = new OpenApiConfig().ragAssistantOpenAPI();
        assertThat(api).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("RAG Research Assistant API");
        assertThat(api.getInfo().getVersion()).isEqualTo("0.1.0");
    }
}
