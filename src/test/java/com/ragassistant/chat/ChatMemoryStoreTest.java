package com.ragassistant.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMemoryStoreTest {

    private final ChatMemoryStore store = new ChatMemoryStore();

    @Test
    void history_emptyForUnknownSession() {
        assertThat(store.history("nope")).isEmpty();
    }

    @Test
    void remember_storesAndHistoryReturnsCopy() {
        store.remember("s", UserMessage.from("hi"), AiMessage.from("hello"));
        List<dev.langchain4j.data.message.ChatMessage> h = store.history("s");
        assertThat(h).hasSize(2);
        assertThat(h.get(0)).isInstanceOf(UserMessage.class);

        store.drop("s");
        assertThat(store.history("s")).isEmpty();
    }

    @Test
    void remember_trimsToMaxMessages() {
        dev.langchain4j.data.message.ChatMessage[] msgs = new dev.langchain4j.data.message.ChatMessage[25];
        for (int i = 0; i < 25; i++) {
            msgs[i] = UserMessage.from("m" + i);
        }
        store.remember("s", msgs);
        assertThat(store.history("s")).hasSize(20);
    }
}
