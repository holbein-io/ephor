# ADR-001: Drop namespace filter from bulk plan filters

## Status

Accepted

## Date

2026-02-04

## Context

Bulk plans apply actions to multiple vulnerabilities in a triage session based on
filter criteria. One proposed filter was `namespace`, which would restrict the bulk
action to vulnerabilities appearing in workloads within a specific Kubernetes namespace.

The data path to resolve namespace is:

```
TriagePreparation -> Vulnerability -> VulnerabilityInstance[] -> Workload -> namespace
```

This requires traversing two additional relationships (instances and workloads) beyond
what the filter matcher normally needs (preparation + vulnerability). A single
vulnerability can appear in multiple workloads across different namespaces, making
the semantics ambiguous: should the filter match if *any* instance is in the
namespace, or *all* of them?

Additionally, triage sessions are global -- they cover all vulnerabilities regardless
of namespace. Namespace-based filtering at the bulk plan level conflates infrastructure
topology with vulnerability triage decisions.

## Decision

Drop `namespace` from the supported bulk plan filter keys. The filter matcher will
silently ignore any `namespace` key found in stored JSONB (lenient matching design).

## Consequences

- Simpler filter resolution: matcher only needs `TriagePreparation` and its
  `Vulnerability`, no deep traversal to instances/workloads
- Old plans that may have stored a `namespace` filter value in JSONB will not break;
  the key is simply ignored
- If namespace-based filtering is needed in the future, it can be added as a new
  filter key with explicit match semantics (any-instance vs all-instances) without
  breaking existing plans
- Frontend filter form drops the namespace input field
