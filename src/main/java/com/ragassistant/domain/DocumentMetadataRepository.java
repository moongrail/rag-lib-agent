package com.ragassistant.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, UUID> {

    List<DocumentMetadata> findByTenantId(String tenantId);

    java.util.Optional<DocumentMetadata> findByTenantIdAndId(String tenantId, java.util.UUID id);

    List<DocumentMetadata> findByTenantIdAndStatus(String tenantId, DocumentStatus status);
}
