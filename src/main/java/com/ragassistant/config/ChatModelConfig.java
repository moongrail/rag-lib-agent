package com.ragassistant.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfig {

    @Bean
    public ChatModel chatModel(AppProperties props) {
        String provider = props.getChat().getProvider().toLowerCase();
        return switch (provider) {
            case "lmstudio" -> lmStudioModel(props.getChat().getLmstudio());
            case "openrouter" -> openRouterModel(props.getChat().getOpenrouter());
            default -> throw new IllegalArgumentException("Unknown chat provider: " + provider);
        };
    }

    private ChatModel openRouterModel(AppProperties.Chat.OpenRouter o) {
        return OpenAiChatModel.builder()
                .baseUrl(o.getBaseUrl())
                .apiKey(o.getApiKey())
                .modelName(o.getModelName())
                .temperature(o.getTemperature())
                .maxTokens(o.getMaxTokens())
                .build();
    }

    private ChatModel lmStudioModel(LmStudio o) {
        return OpenAiChatModel.builder()
                .baseUrl(o.getBaseUrl())
                .apiKey(o.getApiKey())
                .modelName(o.getModelName())
                .temperature(o.getTemperature())
                .maxTokens(o.getMaxTokens())
                .build();
    }
}
