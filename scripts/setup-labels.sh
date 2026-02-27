#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <owner/repo> [--scanner]"
  echo ""
  echo "Replace GitHub default labels with a curated namespaced set."
  echo ""
  echo "Options:"
  echo "  --scanner    Use scanner-specific area labels instead of ephor area labels"
  exit 1
}

if [[ $# -lt 1 ]]; then
  usage
fi

REPO="$1"
shift

SCANNER=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --scanner) SCANNER=true; shift ;;
    *) echo "Unknown option: $1"; usage ;;
  esac
done

if ! command -v gh &>/dev/null; then
  echo "Error: gh CLI is not installed." >&2
  exit 1
fi

echo "==> Deleting existing labels from ${REPO}..."
gh label list --repo "$REPO" --limit 100 --json name --jq '.[].name' | while IFS= read -r label; do
  echo "    Deleting: ${label}"
  gh label delete "$label" --repo "$REPO" --yes
done

create_label() {
  local name="$1" color="$2" description="$3"
  echo "    Creating: ${name}"
  gh label create "$name" --repo "$REPO" --color "$color" --description "$description"
}

echo "==> Creating shared labels..."
create_label "type/bug"          "d73a4a" "Something is not working correctly"
create_label "type/feature"      "0075ca" "New functionality or capability"
create_label "type/docs"         "0e8a16" "Documentation only changes"
create_label "type/chore"        "c5def5" "Maintenance, dependencies, CI"
create_label "type/security"     "b60205" "Security-related issue or fix"

create_label "priority/critical" "b60205" "Must be fixed immediately"
create_label "priority/high"     "d93f0b" "Should be fixed in the current cycle"
create_label "priority/medium"   "fbca04" "Normal priority"
create_label "priority/low"      "c2e0c6" "Nice to have, no urgency"

create_label "status/needs-triage" "ededed" "Awaiting initial review"
create_label "status/blocked"      "f9d0c4" "Blocked by dependency or external factor"
create_label "status/wontfix"      "ffffff" "Intentionally will not be addressed"

create_label "good first issue"  "7057ff" "Good for newcomers"
create_label "help wanted"       "008672" "Extra attention is needed"

if [[ "$SCANNER" == true ]]; then
  echo "==> Creating scanner area labels..."
  create_label "area/scanner" "1d76db" "Core scanner logic"
  create_label "area/helm"    "1d76db" "Helm chart and Kubernetes deployment"
  create_label "area/ci"      "1d76db" "CI/CD pipelines and workflows"
else
  echo "==> Creating ephor area labels..."
  create_label "area/api"       "1d76db" "API module (Spring Boot)"
  create_label "area/dashboard" "1d76db" "Dashboard module (React)"
  create_label "area/helm"      "1d76db" "Helm chart and Kubernetes deployment"
  create_label "area/ci"        "1d76db" "CI/CD pipelines and workflows"
fi

echo ""
echo "==> Final label list for ${REPO}:"
gh label list --repo "$REPO" --limit 100
