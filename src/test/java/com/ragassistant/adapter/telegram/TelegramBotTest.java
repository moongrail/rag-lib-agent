package com.ragassistant.adapter.telegram;

import com.ragassistant.api.dto.ChatResponse;
import com.ragassistant.chat.ChatService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramBotTest {

    private final ChatService chatService = mock(ChatService.class);

    private TelegramBot botWithCaptor(String allowed, AtomicReference<String> sent) {
        return new TelegramBot("token", allowed, chatService) {
            @Override
            public java.io.Serializable execute(BotApiMethod method) throws TelegramApiException {
                if (method instanceof SendMessage sm) {
                    sent.set(sm.getText());
                }
                return new Message();
            }
        };
    }

    private Update update(Long chatId, String text) {
        Chat chat = new Chat();
        chat.setId(chatId);
        Message msg = new Message();
        msg.setChat(chat);
        msg.setText(text);
        Update update = new Update();
        update.setMessage(msg);
        return update;
    }

    @Test
    void startCommand_sendsGreeting() {
        AtomicReference<String> sent = new AtomicReference<>();
        TelegramBot bot = botWithCaptor("111", sent);
        bot.onUpdateReceived(update(111L, "/start"));
        assertThat(sent.get()).contains("Send me a question");
    }

    @Test
    void allowedChat_forwardsToChatService() {
        AtomicReference<String> sent = new AtomicReference<>();
        TelegramBot bot = botWithCaptor("111", sent);
        when(chatService.ask(anyString(), anyString(), anyString()))
                .thenReturn(new ChatResponse("s", "the answer", List.of()));
        bot.onUpdateReceived(update(111L, "question here"));
        assertThat(sent.get()).isEqualTo("the answer");
        verify(chatService).ask("111", "111", "question here");
    }

    @Test
    void disallowedChat_ignores() {
        AtomicReference<String> sent = new AtomicReference<>();
        TelegramBot bot = botWithCaptor("111", sent);
        bot.onUpdateReceived(update(999L, "hello"));
        assertThat(sent.get()).isNull();
        verify(chatService, never()).ask(anyString(), anyString(), anyString());
    }

    @Test
    void noMessage_doesNothing() {
        AtomicReference<String> sent = new AtomicReference<>();
        TelegramBot bot = botWithCaptor("111", sent);
        bot.onUpdateReceived(new Update());
        assertThat(sent.get()).isNull();
    }

    @Test
    void chatServiceError_sendsErrorMessage() {
        AtomicReference<String> sent = new AtomicReference<>();
        TelegramBot bot = botWithCaptor("111", sent);
        when(chatService.ask(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));
        bot.onUpdateReceived(update(111L, "q"));
        assertThat(sent.get()).contains("Error: boom");
    }

    @Test
    void botTokenAndUsername() {
        TelegramBot bot = botWithCaptor("111", new AtomicReference<>());
        assertThat(bot.getBotToken()).isEqualTo("token");
        assertThat(bot.getBotUsername()).isEqualTo("rag_assistant");
    }
}
