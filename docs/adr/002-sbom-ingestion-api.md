# ADR-002: SBOM Ingestion API

## Status
Proposed

## Date
2026-04-03

## Context
The ephor-scanner is being extended to generate CycloneDX/SPDX SBOMs alongside
vulnerability scans (see scanner ADR-016). The platform needs an API endpoint to
receive, store, and serve these SBOM documents.

SBOMs are fundamentally different from vulnerability scan results:
- They are **standard documents** (CycloneDX JSON, SPDX JSON) -- not a custom schema
- They represent the **complete software inventory** of an image, not just vulnerabilities
- A single SBOM per image can be 200 KB - 2 MB (gzip-compressed to 20-200 KB)
- They are **per-image**, not per-workload or per-namespace

The existing `/api/v1/scans/ingest` endpoint is designed for vulnerability data grouped
by namespace and workload. Mixing SBOM documents into this payload would conflate two
different data models and significantly increase payload size for all consumers, even
those that do not use SBOMs.

## Decision
Introduce a dedicated SBOM ingest endpoint and storage model, separate from vulnerability
scan ingestion.

### API Endpoint

#### Ingest
```
POST /api/v1/sbom/ingest
Content-Encoding: gzip
Content-Type: application/json

{
  "image_reference": "nginx:1.25.3",
  "image_digest": "sha256:abc123...",
  "scan_group_id": "uuid",
  "format": "cyclonedx",
  "sbom": { ... raw CycloneDX/SPDX document ... }
}
```

The `sbom` field contains the complete, unmodified SBOM document as produced by Trivy.
The platform stores it as-is for compliance export, and optionally indexes it for
querying (see ADR-003).

#### Retrieval
```
GET /api/v1/sbom/{image_reference}
Accept: application/vnd.cyclonedx+json, application/spdx+json

Returns: latest SBOM for the given image reference
```

```
GET /api/v1/sbom/{image_reference}/history
Returns: list of SBOM versions with timestamps and scan_group_ids
```

### Storage Model

#### Raw SBOM Storage
Store the raw SBOM document in object storage (S3/GCS/MinIO) or as a JSONB column,
keyed by image reference + content hash.

**Deduplication**: Hash the SBOM content (SHA-256). If the same image produces an
identical SBOM across scan runs, store only one copy and update the `last_seen`
timestamp. This prevents unbounded storage growth for stable images.

#### SBOM Metadata Table
```sql
CREATE TABLE sbom_documents (
    id              UUID PRIMARY KEY,
    image_reference TEXT NOT NULL,
    image_digest    TEXT,
    content_hash    TEXT NOT NULL,         -- SHA-256 of raw SBOM
    format          TEXT NOT NULL,         -- 'cyclonedx' | 'spdx-json'
    scan_group_id   UUID,                 -- correlates with vulnerability scan
    first_seen      TIMESTAMPTZ NOT NULL,
    last_seen       TIMESTAMPTZ NOT NULL,
    document        JSONB,                -- raw SBOM (or object storage reference)
    UNIQUE(image_reference, content_hash)
);
```

The `UNIQUE(image_reference, content_hash)` constraint enforces deduplication. On
conflict, only `last_seen` and `scan_group_id` are updated.

### Authentication
Uses the same authentication mechanism as the scan ingest endpoint (custom auth
headers, see scanner ADR-011).

### Payload Handling
- Scanner sends gzip-compressed payloads; API decompresses on ingest
- Maximum uncompressed payload size: 5 MB per SBOM (covers even the largest images)
- Validation: verify the `format` field matches the document structure (basic schema
  check, not full validation)

## Consequences

### Positive
- Clean separation between vulnerability data and SBOM documents
- Standard SBOM documents stored as-is -- no lossy transformation
- Content-hash deduplication keeps storage bounded
- Correlation with vulnerability scans via scan_group_id
- SBOM retrieval API enables compliance export from the dashboard
- Independent evolution: SBOM storage can be optimized without affecting vuln ingestion

### Negative
- New endpoint to implement, test, and maintain
- Additional storage (mitigated by deduplication and compression)
- Scanner must make an additional API call per image when SBOM is enabled

### Mitigations
- Deduplication ensures storage grows only when image contents actually change
- Object storage can be used for large documents, keeping the database lean
- Batch ingest endpoint can be added later if per-image calls become a bottleneck

## Alternatives Considered

### Embed SBOMs in the Existing Scan Ingest Payload
Add an `sbom` field to the existing `ContainerData` in the scan ingest payload.
Rejected because:
- Dramatically increases payload size for all consumers
- Mixes two different data models (vulnerability results vs. software inventory)
- SBOM is per-image but the payload is per-workload -- the same SBOM would be
  duplicated across workloads sharing an image
- Breaks existing API consumers that do not expect SBOM data

### Store SBOMs Only in Object Storage (No Database)
Use S3/GCS exclusively with no database record. Rejected because:
- Cannot efficiently query "which images have an SBOM" or correlate with scans
- No deduplication tracking
- Dashboard would need to probe object storage directly
