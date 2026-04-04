-- SBOM document storage for ingested CycloneDX/SPDX documents
CREATE TABLE sbom_documents (
    id              UUID PRIMARY KEY,
    image_reference TEXT NOT NULL,
    image_digest    TEXT,
    content_hash    TEXT NOT NULL,
    format          TEXT NOT NULL,
    scan_group_id   UUID,
    first_seen      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    document        JSONB,
    UNIQUE(image_reference, content_hash)
);

CREATE INDEX idx_sbom_documents_image_ref ON sbom_documents(image_reference);
CREATE INDEX idx_sbom_documents_scan_group ON sbom_documents(scan_group_id);
CREATE INDEX idx_sbom_documents_latest ON sbom_documents(image_reference, last_seen DESC);
