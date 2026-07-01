package com.ragassistant.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMetadataTest {

    @Test
    void allAccessors() {
        DocumentMetadata m = new DocumentMetadata();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        m.setId(id);
        m.setTenantId("t");
        m.setTitle("title");
        m.setFileName("f.pdf");
        m.setContentType("application/pdf");
        m.setStoragePath("/p/f.pdf");
        m.setStatus(DocumentStatus.INGESTED);
        m.setChunkCount(5);
        m.setErrorMessage("err");
        m.setCreatedBy("user");
        m.setCreatedAt(now);
        m.setUpdatedAt(now);

        assertThat(m.getId()).isEqualTo(id);
        assertThat(m.getTenantId()).isEqualTo("t");
        assertThat(m.getTitle()).isEqualTo("title");
        assertThat(m.getFileName()).isEqualTo("f.pdf");
        assertThat(m.getContentType()).isEqualTo("application/pdf");
        assertThat(m.getStoragePath()).isEqualTo("/p/f.pdf");
        assertThat(m.getStatus()).isEqualTo(DocumentStatus.INGESTED);
        assertThat(m.getChunkCount()).isEqualTo(5);
        assertThat(m.getErrorMessage()).isEqualTo("err");
        assertThat(m.getCreatedBy()).isEqualTo("user");
        assertThat(m.getCreatedAt()).isEqualTo(now);
        assertThat(m.getUpdatedAt()).isEqualTo(now);
    }
}
