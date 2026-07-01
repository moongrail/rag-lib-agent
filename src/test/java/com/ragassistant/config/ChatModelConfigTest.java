package com.ragassistant.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatModelConfigTest {

    private final ChatModelConfig config = new ChatModelConfig();

    private AppProperties propsWith(String provider) {
        AppProperties p = new AppProperties();
        p.getChat().setProvider(provider);
        return p;
    }

    @Test
    void openrouter_returnsOpenAiChatModel() {
        ChatModel model = config.chatModel(propsWith("openrouter"));
        assertThat(model).isInstanceOf(OpenAiChatModel.class);
    }

    @Test
    void lmstudio_returnsOpenAiChatModel() {
        ChatModel model = config.chatModel(propsWith("lmstudio"));
        assertThat(model).isInstanceOf(OpenAiChatModel.class);
    }

    @Test
    void unknownProvider_throws() {
        assertThatThrownBy(() -> config.chatModel(propsWith("anthropic")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
