# ADR-007: Unreported CVEs and live-fleet scoping of SBOM queries

## Status

Accepted

## Date

2026-09-13

## Context

ADR-003 introduced "pre-scan alerts": known CRITICAL/HIGH CVEs matched by exact package
name and version against every stored SBOM, intended to flag vulnerable packages in
images the scanner had not reached yet. After a month in a production cluster the
dashboard reported 16 312 alerts. Measured on 2026-09-11:

| Metric | Value |
|---|---|
| Dashboard headline (`countPreScanAlerts`) | 16 312 |
| Distinct (CVE, image) pairs | 11 249 |
| ...on images currently in `containers` | 16 |
| ...KEV-listed | 5 |
| SBOM image references / deployed image references / overlap | 902 / 382 / 362 |
| SBOM references for images no longer deployed | 540 |

Three mechanisms produce the gap between 16 312 and 16:

1. **Dead image tags.** `WorkloadIngestionService.upsertContainer` updates a container's
   image tag in place, so a deploy removes the old tag from `containers` immediately.
   `sbom_documents` is never pruned. The alert query joined only the SBOM side, so ~11 200
   of the alerts were on image tags nobody runs.
2. **Wrong exclusion predicate.** "No `vulnerability_instance` for this CVE on this image"
   cannot distinguish *not scanned* from *scanned and not reported* from *not deployed*.
   The scanner posts SBOMs and scan results in the same run (shared `scan_group_id`), so
   the *not scanned* population - the one the feature was named after - is empty.
3. **Double counting.** `vulnerabilities` is unique on
   `(cve_id, package_name, package_version, scanner_type)`; one CVE hitting two package
   rows on one image was counted twice (~5 000 rows).

Two further defects made the remainder unusable: ordering by `severity, cve_id` put the
oldest CVE ids first, and the KEV/EPSS enrichment from ADR-004 was not applied.

The name+exact-version cross-match itself is sound: only 16 disagreements with the
scanner across 362 scanned images.

The same unscoped population affects the Software Inventory tabs. Package search, top
packages and license distribution query every historical SBOM document for every image
ever seen, while coverage and the alert query scope to the latest document per image.
The tabs disagree with each other, and a search for `spring-boot` returns 5 526 rows.

## Decision

1. **Live-fleet scoping is the default population for every SBOM-derived query.** An
   image is live when `concat(containers.image_name, ':', containers.image_tag)` equals
   `sbom_documents.image_reference`, and only the latest document per image is
   considered. Inventory tabs (Packages, Licenses, SBOMs) expose the historical
   population through an explicit `include_undeployed` parameter, off by default.

2. **"Pre-scan alerts" are renamed "Unreported CVEs"** and redefined as: a known CVE
   matches a package version in the latest SBOM of a running image, and the scanner did
   not report that CVE on any container running the image. This is a scanner-coverage
   signal. The population is small and reviewable. An unreported CVE on an image nobody
   runs is not a finding, so this list has no `include_undeployed` option.

3. **Grain is `(cve_id, image_reference)`.** Package rows and workloads are aggregated
   into the row. The exclusion predicate is CVE-level, so a second scanner type cannot
   re-flag what the first reported.

4. **Ranking is KEV, then EPSS, then severity, then published date.** The default
   population is `severity IN (CRITICAL, HIGH) OR kev_listed`. The dashboard headline is
   the KEV-listed count on running workloads; the total is secondary.

5. **Old SBOM documents are not deleted as a side effect of scoping.** Retention is a
   separate scheduled job, disabled by default, configured per deployment.

6. **No compatibility shims.** Endpoints and DTOs are renamed outright.

Retroactive matching of *newly published* CVEs against stored SBOMs - the capability
ADR-003 actually promised - requires an external CVE feed. `vulnerabilities` only holds
CVEs the scanner already found somewhere in the fleet. That work is deferred to a future
ADR and builds on the same scoped queries.

## Consequences

- The Unreported CVEs count drops from five digits to the true live-fleet figure. The
  list becomes a review queue rather than a counter.
- Package search, top packages and license distribution agree with coverage and with
  each other. Historical questions remain answerable through `include_undeployed`.
- The alert and inventory queries share one definition of "live" and "latest"; a
  change to either applies everywhere.
- Existing tests that encoded the old "unscanned image" semantics are rewritten; the
  test for the patched-version and latest-SBOM cases stay valid.
- `sbom_documents` keeps growing until the retention job is enabled. Deployments that
  ran before this ADR should run `scripts/cleanup-sbom-bloat.sql` once before enabling it.
