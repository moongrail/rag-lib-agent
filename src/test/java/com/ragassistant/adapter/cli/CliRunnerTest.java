package com.ragassistant.adapter.cli;

import com.ragassistant.chat.ChatService;
import com.ragassistant.domain.DocumentMetadata;
import com.ragassistant.domain.DocumentStatus;
import com.ragassistant.ingestion.DocumentIngestionService;
import com.ragassistant.api.dto.ChatResponse;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CliRunnerTest {

    private final DocumentIngestionService ingestion = mock(DocumentIngestionService.class);
    private final ChatService chat = mock(ChatService.class);
    private final CliRunner runner = new CliRunner(ingestion, chat);

    @Test
    void exitCommand_signalsExit() {
        assertThat(runner.handle("exit")).isEqualTo("__EXIT__");
        assertThat(runner.handle("quit")).isEqualTo("__EXIT__");
    }

    @Test
    void unknownCommand_returnsHelp() {
        assertThat(runner.handle("foobar baz")).contains("Unknown command");
    }

    @Test
    void uploadWithNoPaths_returnsMessage() {
        assertThat(runner.handle("upload ")).isEqualTo("No files specified.");
    }

    @Test
    void uploadIngestsPathsAndReportsCount() {
        DocumentMetadata ok = new DocumentMetadata();
        ok.setStatus(DocumentStatus.INGESTED);
        DocumentMetadata bad = new DocumentMetadata();
        bad.setStatus(DocumentStatus.FAILED);
        when(ingestion.ingestAllPaths(any(), anyString(), anyString())).thenReturn(List.of(ok, bad));

        String result = runner.handle("upload /tmp/a.txt /tmp/b.txt");
        assertThat(result).isEqualTo("Ingested 1/2 file(s).");
        verify(ingestion).ingestAllPaths(any(), anyString(), anyString());
    }

    @Test
    void uploadError_returnsErrorMessage() {
        when(ingestion.ingestAllPaths(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));
        assertThat(runner.handle("upload /tmp/x.txt")).contains("Error: boom");
    }

    @Test
    void askDelegatesToChatService() {
        when(chat.ask(anyString(), any(), anyString())).thenReturn(new ChatResponse("s", "the answer", List.of()));
        assertThat(runner.handle("ask what is this?")).isEqualTo("the answer");
    }

    @Test
    void askError_returnsErrorMessage() {
        when(chat.ask(anyString(), any(), anyString())).thenThrow(new RuntimeException("nope"));
        assertThat(runner.handle("ask q")).contains("Error: nope");
    }

    @Test
    void uploadParsesMultipleSpaceSeparatedPaths() {
        when(ingestion.ingestAllPaths(any(), anyString(), anyString())).thenReturn(List.of());
        runner.handle("upload a.txt   b.txt\tc.txt");
        verify(ingestion).ingestAllPaths(any(List.class), anyString(), anyString());
    }
}
