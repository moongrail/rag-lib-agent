package com.ragassistant.adapter.telegram;

import com.ragassistant.chat.ChatService;
import com.ragassistant.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramConfigTest {

    private final TelegramConfig config = new TelegramConfig();

    private AppProperties telegramEnabled() {
        AppProperties p = new AppProperties();
        p.getTelegram().setEnabled(true);
        p.getTelegram().setToken("tok");
        p.getTelegram().setAllowedChatIds("1,2");
        return p;
    }

    @Test
    void telegramBot_beanCreated() {
        ChatService chat = Mockito.mock(ChatService.class);
        TelegramBot bot = config.telegramBot(telegramEnabled(), chat);
        assertThat(bot).isNotNull();
        assertThat(bot.getBotToken()).isEqualTo("tok");
        assertThat(bot.getBotUsername()).isEqualTo("rag_assistant");
    }

    @Test
    void telegramRegistration_beanCreated() {
        ChatService chat = Mockito.mock(ChatService.class);
        TelegramBot bot = config.telegramBot(telegramEnabled(), chat);
        assertThat(config.telegramRegistration(bot)).isNotNull();
    }
}
