package com.ragassistant.adapter.telegram;

import com.ragassistant.chat.ChatService;
import com.ragassistant.config.AppProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Configuration
@Profile("telegram")
public class TelegramConfig {

    @Bean
    public TelegramBot telegramBot(AppProperties props, ChatService chatService) {
        return new TelegramBot(
                props.getTelegram().getToken(),
                props.getTelegram().getAllowedChatIds(),
                chatService);
    }

    @Bean
    public ApplicationRunner telegramRegistration(TelegramBot bot) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) throws Exception {
                try {
                    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                    botsApi.registerBot(bot);
                } catch (TelegramApiException e) {
                    throw new IllegalStateException("Failed to register Telegram bot", e);
                }
            }
        };
    }
}
