package com.ragassistant.adapter.rest;

import com.ragassistant.api.dto.DocumentSummary;
import com.ragassistant.api.dto.UploadResponse;
import com.ragassistant.common.TenantContext;
import com.ragassistant.domain.DocumentMetadata;
import com.ragassistant.domain.DocumentMetadataRepository;
import com.ragassistant.ingestion.DocumentIngestionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final DocumentMetadataRepository metadataRepository;

    public DocumentController(DocumentIngestionService ingestionService,
                              DocumentMetadataRepository metadataRepository) {
        this.ingestionService = ingestionService;
        this.metadataRepository = metadataRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String tenant = TenantContext.get();
        DocumentMetadata meta = ingestionService.ingest(file, tenant, tenant);
        return ResponseEntity.ok(new UploadResponse(meta.getId(), meta.getFileName(), meta.getStatus().name()));
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<UploadResponse>> uploadBulk(@RequestParam("files") List<MultipartFile> files) {
        String tenant = TenantContext.get();
        List<DocumentMetadata> metas = ingestionService.ingestAll(files, tenant, tenant);
        List<UploadResponse> responses = metas.stream()
                .map(m -> new UploadResponse(m.getId(), m.getFileName(), m.getStatus().name()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    public List<DocumentSummary> list() {
        String tenant = TenantContext.get();
        return metadataRepository.findByTenantId(tenant).stream()
                .map(m -> new DocumentSummary(m.getId(), m.getTitle(), m.getFileName(),
                        m.getStatus().name(), m.getChunkCount(), m.getCreatedAt()))
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentSummary get(@PathVariable UUID id) {
        String tenant = TenantContext.get();
        DocumentMetadata m = metadataRepository.findByTenantIdAndId(tenant, id)
                .orElseThrow(() -> new com.ragassistant.common.exceptions.DocumentNotFoundException(id.toString()));
        return new DocumentSummary(m.getId(), m.getTitle(), m.getFileName(),
                m.getStatus().name(), m.getChunkCount(), m.getCreatedAt());
    }
}
