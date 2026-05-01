## Status
Proposed

## Date
2026-04-03

## Context
With SBOM documents ingested and stored (ADR-002), the platform needs to decide how
to present SBOM data in the dashboard. The core question is whether SBOMs serve as
downloadable compliance artifacts, a queryable software inventory, or both.

The competitive landscape shows:
- **Paid platforms** (Wiz, Snyk, Prisma) treat SBOMs primarily as a compliance export
  feature -- generate, download, done. Queryable inventory and retroactive CVE matching
  are premium features with limited open-source alternatives.
- **Open-source tools** (Dependency-Track, Grype) provide SBOM ingestion and basic
  querying but lack Kubernetes workload context and runtime correlation.

The highest-value capabilities that SBOMs unlock on the platform are:

1. **Retroactive CVE matching** -- when a new CVE is published, instantly identify
   affected images from stored SBOMs without rescanning
2. **Cross-fleet dependency search** -- "which images contain log4j?" across all
   namespaces and clusters
3. **License compliance surface** -- "which workloads run AGPL dependencies in
   production?"
4. **SBOM drift detection** -- "this image tag's package contents changed between scans"
5. **Compliance export** -- download standard CycloneDX/SPDX per image for auditors

## Decision
Implement SBOM presentation in two phases: compliance export first (immediate value,
low effort), then queryable inventory (high value, requires indexing infrastructure).

### Phase 1: Compliance Export (MVP)

Provide SBOM download directly from the dashboard, using the raw documents stored
via ADR-002.

**Dashboard integration points:**
- **Image detail view**: "Download SBOM" button (CycloneDX JSON / SPDX JSON)
- **Workload detail view**: Download SBOMs for all containers in the workload
- **Scan results view**: SBOM availability indicator per image (badge/icon)

**No indexing required** -- Phase 1 reads raw SBOM documents from storage and serves
them as downloads. The dashboard displays SBOM metadata (format, generation date,
package count parsed from the document) but does not offer search or filtering by
package.

This covers the compliance use case: auditors and procurement teams can request and
receive standard SBOM documents for any scanned image.

### Phase 2: Queryable Inventory

Index SBOM package data into a searchable structure to enable cross-fleet queries.

**Package index table:**
```sql
CREATE TABLE sbom_packages (
    id              UUID PRIMARY KEY,
    sbom_id         UUID REFERENCES sbom_documents(id),
    image_reference TEXT NOT NULL,
    purl            TEXT,                 -- package URL (pkg:npm/express@4.18.2)
    name            TEXT NOT NULL,
    version         TEXT NOT NULL,
    type            TEXT,                 -- npm, maven, debian, alpine, etc.
    license         TEXT,                 -- SPDX license expression
    supplier        TEXT
);

CREATE INDEX idx_sbom_packages_name ON sbom_packages(name);
CREATE INDEX idx_sbom_packages_purl ON sbom_packages(purl);
CREATE INDEX idx_sbom_packages_image ON sbom_packages(image_reference);
```

**Dashboard features:**
- **Global package search**: "Find all images containing package X" with version
  filtering
- **License audit view**: Filter workloads by license type across namespaces
- **SBOM diff**: Compare two SBOM versions for the same image -- show added, removed,
  and changed packages
- **Dependency frequency**: "Top 20 most common packages across the fleet"

**Retroactive CVE matching:**
When a new CVE advisory is ingested (from Trivy DB updates or external feeds), query
the package index to identify affected images without triggering a rescan. Surface
these as "pre-scan alerts" in the dashboard -- informing users before the next
scheduled scan confirms the finding.

### What Stays Out of Scope
- **Dependency graph visualization** -- full dependency tree rendering adds significant
  frontend complexity for limited triage value. Defer until user demand is clear.
- **SBOM signing/attestation** (cosign/sigstore) -- valuable for supply chain
  verification but orthogonal to dashboard presentation. Can be added independently.
- **SBOM generation on the platform side** -- the scanner generates SBOMs; the platform
  consumes them. No server-side SBOM generation.

## Consequences

### Positive
- Phase 1 delivers immediate compliance value with minimal implementation effort
- Phase 2 provides capabilities that differentiate from paid platforms at the
  open-source tier (retroactive CVE matching, cross-fleet search)
- Phased approach allows validating demand before investing in indexing infrastructure
- Package index enables future features (license policy enforcement, dependency
  deprecation alerts) without re-architecting

### Negative
- Phase 2 indexing adds a new table and write path during SBOM ingestion
- Package index grows with fleet size (500 images x 500 packages = 250K rows) --
  manageable for Postgres but needs monitoring
- Retroactive CVE matching requires a CVE feed integration beyond Trivy DB

### Mitigations
- Phase 2 indexing can be async (background job after SBOM ingest, not blocking)
- Package index can be rebuilt from stored raw SBOMs if schema evolves
- Start retroactive matching with Trivy's own DB (already cached) before adding
  external feeds

## Alternatives Considered

### Index-Only, No Raw Storage
Parse SBOMs on ingest and only store the indexed package data, discarding the raw
document. Rejected because:
- Loses compliance export capability (auditors want the original document)
- Lossy: not all SBOM fields are indexed
- Cannot re-index if schema evolves

### Full Inventory from Day One (No Phasing)
Build both export and queryable inventory in a single release. Rejected because:
- Phase 1 alone covers the most pressing compliance need
- Phasing allows gathering user feedback on what queries matter most
- Reduces time-to-value for the initial SBOM feature

### Integrate Dependency-Track Instead of Custom Indexing
Use OWASP Dependency-Track as the SBOM management backend. Rejected because:
- Adds a significant operational dependency (separate Java service + database)
- Duplicates data that the Ephor platform already manages (images, workloads)
- Limits dashboard integration depth -- would need to proxy or iframe
- Viable as a future integration option but not as the core implementation
