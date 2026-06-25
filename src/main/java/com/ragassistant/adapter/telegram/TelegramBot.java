package com.ragassistant.adapter.telegram;

import com.ragassistant.chat.ChatService;
import com.ragassistant.config.AppProperties;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.Set;

public class TelegramBot extends TelegramLongPollingBot {

    private final String token;
    private final Set<String> allowedChatIds;
    private final ChatService chatService;

    public TelegramBot(String token, String allowedChatIdsCsv, ChatService chatService) {
        super(token);
        this.token = token;
        this.allowedChatIds = Arrays.stream(allowedChatIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        this.chatService = chatService;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public String getBotUsername() {
        return "rag_assistant";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String chatId = update.getMessage().getChatId().toString();
        if (!allowedChatIds.isEmpty() && !allowedChatIds.contains(chatId)) {
            return;
        }
        String text = update.getMessage().getText();
        if (text.startsWith("/start")) {
            send(chatId, "Hi! Send me a question and I will answer from your indexed documents.");
            return;
        }
        try {
            var response = chatService.ask(chatId, chatId, text);
            send(chatId, response.answer());
        } catch (Exception e) {
            send(chatId, "Error: " + e.getMessage());
        }
    }

    private void send(String chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}
