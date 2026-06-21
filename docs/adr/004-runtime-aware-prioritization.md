# ADR-004: Runtime-Aware Prioritization (KEV/EPSS Enrichment)

## Status
Proposed

## Date
2026-06-21

## Context

Ephor's defensible wedge is not "another vulnerability dashboard" -- that category is
crowded (OWASP Dependency-Track, DefectDojo in OSS; Wiz/Sysdig/Endor commercially). The
one structural advantage Ephor has is that its companion scanner runs **in-cluster**, so
it knows what is actually **deployed and running right now** -- something Dependency-Track
and DefectDojo structurally cannot see.

The market has moved from "list every CVE" to "show me the handful that matter" (Sysdig:
~2% of findings are in-use + exploitable + fixable; Endor: 92% reduction via reachability;
CSA VulnOps 2026: "risk-based prioritization replaces severity ranking"). Prioritization
is now the product, not a feature.

Prioritization here is the product of three signals:

| Signal | How Ephor derives it | Status today |
|--------|----------------------|--------------|
| **Deployed** | A `VulnerabilityInstance` in status `open` or `triaged` (present in the latest scan; `ScanIngestionService` auto-resolves anything that drops out) | Already modelled |
| **Fixable** | `vulnerabilities.fixed_version` is present | Already modelled |
| **Exploitable** | On CISA KEV, or FIRST EPSS at/above a threshold | **Missing** -- only CVSS exists, which is the severity-ranking the market is leaving |

Two of the three signals already exist; the gap is the **exploitable** signal. This ADR
adds it and defines the prioritization model built on top.

### Honest-claim guardrail
Ephor claims findings are **"deployed/running in your cluster"** (workload/image presence)
-- true and differentiated. It does **not** claim code-level **reachability** ("is the
vulnerable function actually loaded/executed"); that is eBPF-class capability (Sysdig,
Endor) that a Trivy-based scanner cannot provide. KEV/EPSS express *exploitation
likelihood in the wild*, not exploitation in this environment.

## Decision

Add a KEV/EPSS enrichment subsystem and a deployed x exploitable x fixable prioritization
model. Delivery is phased; **this ADR's build scope is Phase 1 only** (get the signal
flowing and visible). Phases 2-3 are documented here for direction but deferred.

### Prioritization model (target)

A finding's **exploitable** flag is: `kev_listed = true` OR `epss_score >= 0.10`
(default threshold, configurable). KEV is always exploitable regardless of EPSS.

Findings resolve to a discrete **priority tier**, EPSS as the in-tier tiebreak:

| Tier | Rule |
|------|------|
| **P0 -- Act now** | deployed AND KEV AND fixable |
| **P1 -- Urgent**  | deployed AND (KEV OR EPSS >= threshold) AND fixable |
| **P2 -- Plan**    | deployed AND exploitable, no fix yet |
| **P3 -- Monitor** | everything else |

Within a tier: sort by `epss_score` desc, then `cvss_v3_score` desc.

Tiers (not a blended numeric score) are deliberate: legible, honest, and defensible. A
weighted score invents false precision over arbitrary weights.

Priority is **derived at query time**, never stored: `deployed` changes with every scan,
so a stored tier would go stale. Only the enrichment inputs (KEV/EPSS) are persisted.

### Data model (migration `007_add_vulnerability_enrichment.sql`)

Add to `vulnerabilities` (all nullable; per-CVE):

```sql
ALTER TABLE vulnerabilities
    ADD COLUMN kev_listed           BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN kev_date_added       DATE,
    ADD COLUMN epss_score           DOUBLE PRECISION,
    ADD COLUMN epss_percentile      DOUBLE PRECISION,
    ADD COLUMN enrichment_updated_at TIMESTAMPTZ;
```

`cve_id` is **not** unique on its own (the natural key is
`(cve_id, package_name, package_version, scanner_type)`), so one feed value updates *all*
vuln rows sharing that CVE via `UPDATE ... WHERE cve_id = ?`. The composite unique
constraint already provides a `cve_id`-leading index, so no extra index is required.

No `VulnerabilityInstance` change -- "deployed" is already its status.

### Enrichment subsystem

A new `EnrichmentService`, following the existing directory-sync pattern
(`KeycloakUserDirectoryProvider` / `GitHubUserDirectoryProvider`): a `WebClient` built in
`@PostConstruct`, a `@Scheduled(fixedDelayString = ...)` entry point, and an
`@ConfigurationProperties` class. `@EnableScheduling` is already on.

- **KEV** -- GET the CISA KEV catalog JSON (~1-2k entries). For each `cveID`, capture
  `dateAdded`; bulk-set `kev_listed = true`, `kev_date_added` on matching vulns. Vulns no
  longer in the catalog are reset to `false` (KEV entries are rarely removed, but keep it
  authoritative).
- **EPSS** -- GET FIRST's daily EPSS CSV (gzip; `cve,epss,percentile`, ~250k rows). Match
  only against the CVE ids Ephor tracks (`SELECT DISTINCT cve_id FROM vulnerabilities`);
  bulk-update `epss_score`, `epss_percentile`. We never store the ~250k rows we don't use.
- **Cadence** -- daily (`fixedDelayString` default `86400000`, configurable), plus a run
  shortly after startup. Both feeds are global (not cluster-specific), so this belongs in
  the platform API, not the scanner.
- **Matching** -- by `cve_id`. Trivy findings without a CVE id (GHSA-only) stay
  unenriched (`kev_listed = false`, `epss_score = null`); they fall to P3 unless deployed
  + fixable. Acceptable and explicit.
- **Resilience** -- each feed refresh is independent and transactional; a failed or
  unreachable feed logs and leaves prior values intact (last-known-good), mirroring the directory-sync
  error handling. `enrichment_updated_at` records freshness.

### Configuration

```yaml
ephor:
  enrichment:
    kev-url: ${EPHOR_ENRICHMENT_KEV_URL:https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json}
    epss-url: ${EPHOR_ENRICHMENT_EPSS_URL:https://epss.cyentia.com/epss_scores-current.csv.gz}
    refresh-interval: ${EPHOR_ENRICHMENT_REFRESH_INTERVAL:86400000}
    epss-threshold: ${EPHOR_ENRICHMENT_EPSS_THRESHOLD:0.10}
```

### Scope

**Phase 1 (this ADR -- build now): make the signal exist and visible.**
- Migration `007` + the five enrichment columns on the `Vulnerability` entity.
- `EnrichmentService` + `EnrichmentProperties` + KEV and EPSS feed ingestion.
- Expose `kev_listed`, `kev_date_added`, `epss_score`, `epss_percentile` on
  `VulnerabilityWithAffectedWorkloads` (add `v.kev_listed`, `v.epss_score`,
  `v.epss_percentile` to the list query SELECT + `mapRowToDto`) and on the vulnerability
  detail response.
- Frontend: a **KEV badge** (the strongest single signal) and the **EPSS score** (as a
  percentage) on vulnerability rows and the detail page; extend the TS types and
  `constants/colors.ts`.

Phase 1 deliberately ships *no* tier computation, sort, filter, or dashboard change --
the goal is to get real KEV/EPSS data into the cluster and eyeball it before building UI
on top of it.

**Phase 2 (deferred): prioritize.** Tier as a derived `CASE` expression in the list
query; new `priority` and `epss` sort options (extend `VulnerabilitySortOptions` /
`ALLOWED_SORT_COLUMNS`); new `kev_only` / `deployed_only` / `fixable_only` / `min_epss`
filters; default sort becomes priority.

**Phase 3 (deferred): the headline.** Reframe the namespace heatmap and dashboard from
*severity counts* to *running + exploitable + fixable* counts; a "Priority worklist" hero
("N findings need action now"). This is the positioning payload and should only be built
once Phases 1-2 are validated against live data.

## Consequences

**Positive**
- Closes the only missing input to the prioritization model; turns Ephor's in-cluster
  vantage into a worklist-*subtracting* product instead of another dashboard.
- Both feeds are free and public; no new vendor dependency or cost.
- Reuses existing scheduling/HTTP/config/Liquibase patterns -- no new infrastructure.

**Negative / risks**
- Introduces the platform's first *outbound* dependency on third-party feeds; air-gapped
  installs need configurable URLs / an offline path (URLs are configurable; offline import
  is a possible later addition).
- Enrichment is only as fresh as the last successful pull; `enrichment_updated_at` exposes
  this, and last-known-good avoids blanks on transient failures.
- GHSA-only findings are unenriched -- a coverage gap inherent to CVE-keyed feeds.
- EPSS scores drift daily; a fixed 0.10 threshold may need tuning (hence configurable).

## Alternatives considered

- **Numeric priority score** instead of tiers -- rejected: arbitrary weights, false
  precision, harder to defend on a "the 2% that matter" pitch.
- **Storing a computed priority/tier column** -- rejected: `deployed` changes every scan,
  so any stored tier is immediately stale; derive at query time.
- **Per-CVE EPSS API lookups** instead of the bulk CSV -- rejected: thousands of HTTP
  round-trips vs one daily file; the CSV is the documented bulk distribution.
- **Running enrichment in the scanner** -- rejected: KEV/EPSS are global, not
  cluster-specific; the platform is the right owner (matches the directory-sync model).
</content>
