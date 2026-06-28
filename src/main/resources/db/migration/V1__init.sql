CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS documents (
    id            UUID PRIMARY KEY,
    tenant_id     VARCHAR(100)   NOT NULL,
    title         VARCHAR(512)   NOT NULL,
    file_name     VARCHAR(512)   NOT NULL,
    content_type  VARCHAR(100)   NOT NULL,
    storage_path  VARCHAR(1024),
    status        VARCHAR(20)    NOT NULL,
    chunk_count   INTEGER        NOT NULL DEFAULT 0,
    error_message TEXT,
    created_by    VARCHAR(100),
    created_at    TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_documents_tenant ON documents (tenant_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents (status);
