package com.ragassistant.adapter.cli;

import com.ragassistant.chat.ChatService;
import com.ragassistant.ingestion.DocumentIngestionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CliRunnerRunTest {

    private final InputStream originalIn = System.in;
    private final DocumentIngestionService ingestion = mock(DocumentIngestionService.class);
    private final ChatService chat = mock(ChatService.class);

    @AfterEach
    void restore() {
        System.setIn(originalIn);
    }

    @Test
    void run_processesPipedCommands() {
        String script = "ask what is the refund policy?\nexit\n";
        System.setIn(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)));
        CliRunner runner = new CliRunner(ingestion, chat);
        runner.run();

        verify(chat).ask(eq("cli"), isNull(), eq("what is the refund policy?"));
    }
}
