package com.ragassistant.adapter.rest;

import com.ragassistant.api.dto.DocumentSummary;
import com.ragassistant.api.dto.UploadResponse;
import com.ragassistant.common.TenantContext;
import com.ragassistant.common.TenantIds;
import com.ragassistant.common.exceptions.RagException;
import com.ragassistant.domain.DocumentMetadata;
import com.ragassistant.domain.DocumentMetadataRepository;
import com.ragassistant.ingestion.DocumentIngestionService;
import com.ragassistant.storage.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private static final int MAX_BULK_FILES = 20;

    private final DocumentIngestionService ingestionService;
    private final DocumentMetadataRepository metadataRepository;
    private final DocumentStorageService storageService;

    public DocumentController(DocumentIngestionService ingestionService,
                              DocumentMetadataRepository metadataRepository,
                              DocumentStorageService storageService) {
        this.ingestionService = ingestionService;
        this.metadataRepository = metadataRepository;
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RagException("File must not be empty");
        }
        String tenant = TenantIds.sanitize(TenantContext.get());
        DocumentMetadata meta = ingestionService.ingest(file, tenant, tenant);
        return ResponseEntity.ok(new UploadResponse(meta.getId(), meta.getFileName(), meta.getStatus().name()));
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<UploadResponse>> uploadBulk(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new RagException("At least one file must be provided");
        }
        if (files.size() > MAX_BULK_FILES) {
            throw new RagException("Too many files (max " + MAX_BULK_FILES + ")");
        }
        String tenant = TenantIds.sanitize(TenantContext.get());
        List<DocumentMetadata> metas = ingestionService.ingestAll(files, tenant, tenant);
        List<UploadResponse> responses = metas.stream()
                .map(m -> new UploadResponse(m.getId(), m.getFileName(), m.getStatus().name()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    public List<DocumentSummary> list() {
        String tenant = TenantIds.sanitize(TenantContext.get());
        return metadataRepository.findByTenantId(tenant).stream()
                .map(m -> new DocumentSummary(m.getId(), m.getTitle(), m.getFileName(),
                        m.getStatus().name(), m.getChunkCount(), m.getCreatedAt()))
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentSummary get(@PathVariable UUID id) {
        String tenant = TenantIds.sanitize(TenantContext.get());
        DocumentMetadata m = metadataRepository.findByTenantIdAndId(tenant, id)
                .orElseThrow(() -> new com.ragassistant.common.exceptions.DocumentNotFoundException(id.toString()));
        return new DocumentSummary(m.getId(), m.getTitle(), m.getFileName(),
                m.getStatus().name(), m.getChunkCount(), m.getCreatedAt());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        String tenant = TenantIds.sanitize(TenantContext.get());
        DocumentMetadata m = metadataRepository.findByTenantIdAndId(tenant, id)
                .orElseThrow(() -> new com.ragassistant.common.exceptions.DocumentNotFoundException(id.toString()));
        metadataRepository.delete(m);
        if (m.getStoragePath() != null) {
            try {
                storageService.delete(m.getStoragePath());
            } catch (Exception e) {
                log.warn("Failed to delete stored file {}: {}", m.getStoragePath(), e.getMessage());
            }
        }
        // NOTE: pgvector rows for this docId become orphans. LangChain4j
        // EmbeddingStore has no delete-by-metadata; a purge job (P1, see
        // docs/REVIEW_2026.md) will remove them. Retrieval already filters
        // by tenantId, so orphans are invisible until purged.
        log.info("Document {} (docId={}) metadata deleted; vector purge pending", id, id);
        return ResponseEntity.noContent().build();
    }
}
