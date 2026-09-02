package com.ragassistant.ingestion;

import com.ragassistant.common.exceptions.RagException;
import com.ragassistant.config.AppProperties;
import com.ragassistant.domain.DocumentMetadata;
import com.ragassistant.domain.DocumentMetadataRepository;
import com.ragassistant.domain.DocumentStatus;
import com.ragassistant.embedding.EmbeddingProvider;
import com.ragassistant.storage.DocumentStorageService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class DocumentIngestionService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt", "md", "markdown", "xml");

    private final DocumentStorageService storageService;
    private final EmbeddingProvider embeddingProvider;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentMetadataRepository metadataRepository;
    private final AppProperties appProperties;
    private final DocumentParserFactory parserFactory;

    public DocumentIngestionService(DocumentStorageService storageService,
                                    EmbeddingProvider embeddingProvider,
                                    EmbeddingStore<TextSegment> embeddingStore,
                                    DocumentMetadataRepository metadataRepository,
                                    AppProperties appProperties,
                                    DocumentParserFactory parserFactory) {
        this.storageService = storageService;
        this.embeddingProvider = embeddingProvider;
        this.embeddingStore = embeddingStore;
        this.metadataRepository = metadataRepository;
        this.appProperties = appProperties;
        this.parserFactory = parserFactory;
    }

    public DocumentMetadata ingest(MultipartFile file, String tenantId, String createdBy) {
        DocumentMetadata meta = ingestInternal(file, tenantId, createdBy);
        if (meta.getStatus() == DocumentStatus.FAILED) {
            throw new RagException("Ingestion failed for " + meta.getFileName() + ": " + meta.getErrorMessage());
        }
        return meta;
    }

    public DocumentMetadata ingest(Path path, String tenantId, String createdBy) {
        DocumentMetadata meta = ingestInternal(path, tenantId, createdBy);
        if (meta.getStatus() == DocumentStatus.FAILED) {
            throw new RagException("Ingestion failed for " + meta.getFileName() + ": " + meta.getErrorMessage());
        }
        return meta;
    }

    public List<DocumentMetadata> ingestAll(List<MultipartFile> files, String tenantId, String createdBy) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<DocumentMetadata>> futures = new ArrayList<>(files.size());
            for (MultipartFile file : files) {
                futures.add(executor.submit(() -> ingestInternal(file, tenantId, createdBy)));
            }
            List<DocumentMetadata> result = new ArrayList<>(files.size());
            for (Future<DocumentMetadata> future : futures) {
                try {
                    result.add(future.get());
                } catch (Exception e) {
                    DocumentMetadata failed = new DocumentMetadata();
                    failed.setStatus(DocumentStatus.FAILED);
                    failed.setErrorMessage(e.getMessage());
                    result.add(failed);
                }
            }
            return result;
        }
    }

    public List<DocumentMetadata> ingestAllPaths(List<Path> paths, String tenantId, String createdBy) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<DocumentMetadata>> futures = new ArrayList<>(paths.size());
            for (Path path : paths) {
                futures.add(executor.submit(() -> ingestInternal(path, tenantId, createdBy)));
            }
            List<DocumentMetadata> result = new ArrayList<>(paths.size());
            for (Future<DocumentMetadata> future : futures) {
                try {
                    result.add(future.get());
                } catch (Exception e) {
                    DocumentMetadata failed = new DocumentMetadata();
                    failed.setStatus(DocumentStatus.FAILED);
                    failed.setErrorMessage(e.getMessage());
                    result.add(failed);
                }
            }
            return result;
        }
    }

    private DocumentMetadata ingestInternal(MultipartFile file, String tenantId, String createdBy) {
        try (InputStream in = file.getInputStream()) {
            return ingestInternal(in, file.getOriginalFilename(), file.getContentType(), tenantId, createdBy);
        } catch (IOException e) {
            throw new RagException("Failed to read uploaded file", e);
        }
    }

    private DocumentMetadata ingestInternal(Path path, String tenantId, String createdBy) {
        try (InputStream in = Files.newInputStream(path)) {
            return ingestInternal(in, path.getFileName().toString(), detectContentType(path), tenantId, createdBy);
        } catch (IOException e) {
            throw new RagException("Failed to read file: " + path, e);
        }
    }

    private DocumentMetadata ingestInternal(InputStream content, String fileName, String contentType,
                                            String tenantId, String createdBy) {
        String safeName = fileName == null || fileName.isBlank() ? "unnamed" : fileName.strip();
        DocumentMetadata meta = new DocumentMetadata();
        meta.setTenantId(tenantId);
        meta.setTitle(safeName);
        meta.setFileName(safeName);
        meta.setContentType(contentType);
        meta.setStatus(DocumentStatus.PENDING);
        meta.setChunkCount(0);
        meta.setCreatedBy(createdBy);
        meta = metadataRepository.save(meta);

        if (!isAllowed(safeName)) {
            meta.setStatus(DocumentStatus.FAILED);
            meta.setErrorMessage("Unsupported format: " + safeName);
            return metadataRepository.save(meta);
        }

        String storagePath = null;
        try {
            storagePath = storageService.store(tenantId, safeName, content);
            meta.setStoragePath(storagePath);

            Document document = parse(contentType, safeName, storagePath);
            DocumentSplitter splitter = DocumentSplitters.recursive(
                    appProperties.getIngestion().getChunkSize(),
                    appProperties.getIngestion().getChunkOverlap());
            List<TextSegment> segments = splitter.split(document);

            List<TextSegment> enriched = new ArrayList<>(segments.size());
            String docId = meta.getId() == null ? "" : meta.getId().toString();
            for (TextSegment seg : segments) {
                seg.metadata().put("tenantId", tenantId);
                seg.metadata().put("fileName", safeName);
                seg.metadata().put("docId", docId);
                enriched.add(seg);
            }
            if (enriched.isEmpty()) {
                meta.setStatus(DocumentStatus.FAILED);
                meta.setErrorMessage("Document contains no indexable text: " + safeName);
                metadataRepository.save(meta);
                storageService.delete(storagePath);
                return meta;
            }

            List<Embedding> embeddings = embeddingProvider.embedAll(enriched);
            // Batch write: one round-trip instead of N (P0 perf fix, same semantics).
            embeddingStore.addAll(embeddings, enriched);

            meta.setStatus(DocumentStatus.INGESTED);
            meta.setChunkCount(enriched.size());
            return metadataRepository.save(meta);
        } catch (Exception e) {
            meta.setStatus(DocumentStatus.FAILED);
            meta.setErrorMessage(e.getMessage());
            metadataRepository.save(meta);
            if (storagePath != null) {
                storageService.delete(storagePath);
            }
            return meta;
        }
    }

    private Document parse(String contentType, String fileName, String storagePath) {
        DocumentParser parser = parserFactory.parserFor(contentType, fileName);
        try (InputStream in = storageService.load(storagePath)) {
            return parser.parse(in);
        } catch (Exception e) {
            throw new RagException("Failed to parse document: " + storagePath, e);
        }
    }

    private boolean isAllowed(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        int dot = lower.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(lower.substring(dot + 1));
    }

    private String detectContentType(Path path) {
        try {
            String guessed = Files.probeContentType(path);
            if (guessed != null) {
                return guessed;
            }
        } catch (IOException ignored) {
        }
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (name.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (name.endsWith(".xml")) {
            return "application/xml";
        }
        return "text/plain";
    }
}
