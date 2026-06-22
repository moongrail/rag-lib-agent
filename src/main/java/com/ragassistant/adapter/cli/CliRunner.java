package com.ragassistant.adapter.cli;

import com.ragassistant.chat.ChatService;
import com.ragassistant.common.TenantContext;
import com.ragassistant.domain.DocumentMetadata;
import com.ragassistant.domain.DocumentStatus;
import com.ragassistant.ingestion.DocumentIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@Component
@Profile("cli")
public class CliRunner implements CommandLineRunner {

    private final DocumentIngestionService ingestionService;
    private final ChatService chatService;
    private static final String TENANT = "cli";

    public CliRunner(DocumentIngestionService ingestionService, ChatService chatService) {
        this.ingestionService = ingestionService;
        this.chatService = chatService;
    }

    @Override
    public void run(String... args) {
        TenantContext.set(TENANT);
        Scanner scanner = new Scanner(System.in);
        System.out.println("RAG Assistant CLI. Commands: 'upload <path> [<path> ...]', 'ask <question>', 'exit'");
        while (true) {
            System.out.print("\n> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String result = handle(line);
            if ("__EXIT__".equals(result)) {
                break;
            }
            if (result != null) {
                System.out.println(result);
            }
        }
        scanner.close();
        TenantContext.clear();
    }

    public String handle(String line) {
        if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
            return "__EXIT__";
        }
        if (line.startsWith("upload ")) {
            List<Path> paths = Arrays.stream(line.substring("upload ".length()).trim().split("\\s+"))
                    .filter(s -> !s.isEmpty())
                    .map(Path::of)
                    .toList();
            if (paths.isEmpty()) {
                return "No files specified.";
            }
            try {
                List<DocumentMetadata> results = ingestionService.ingestAllPaths(paths, TENANT, TENANT);
                long ok = results.stream().filter(m -> m.getStatus() == DocumentStatus.INGESTED).count();
                return "Ingested " + ok + "/" + results.size() + " file(s).";
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        if (line.startsWith("ask ")) {
            String question = line.substring("ask ".length()).trim();
            try {
                var response = chatService.ask(TENANT, null, question);
                return response.answer();
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        return "Unknown command. Use 'upload <path> [<path> ...]', 'ask <question>', or 'exit'.";
    }
}
