package com.ragassistant.storage;

import com.ragassistant.common.exceptions.RagException;
import com.ragassistant.config.AppProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final String rootPath;

    public DocumentStorageService(AppProperties appProperties) {
        this.rootPath = appProperties.getStorage().getPath();
    }

    public String store(String tenantId, String originalFileName, InputStream content) {
        try {
            Path tenantDir = Path.of(rootPath, tenantId).toAbsolutePath();
            Files.createDirectories(tenantDir);
            String safeName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = tenantDir.resolve(UUID.randomUUID() + "_" + safeName);
            Files.copy(content, target);
            return target.toString();
        } catch (IOException e) {
            throw new RagException("Failed to store document", e);
        }
    }

    public InputStream load(String storagePath) {
        try {
            return Files.newInputStream(Path.of(storagePath));
        } catch (IOException e) {
            throw new RagException("Failed to load document from storage: " + storagePath, e);
        }
    }

    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(Path.of(storagePath));
        } catch (IOException e) {
            throw new RagException("Failed to delete document from storage: " + storagePath, e);
        }
    }
}
