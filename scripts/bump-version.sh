#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

VERSION_FILES=(
  "build.gradle"
  "api/build.gradle"
  "dashboard/app/package.json"
  "charts/ephor/Chart.yaml"
)

usage() {
  echo "Usage: $0 <version> [--tag] [--dry-run]"
  echo ""
  echo "Bump the version across all ephor packages."
  echo ""
  echo "Arguments:"
  echo "  version    Semantic version without 'v' prefix (e.g. 0.3.0)"
  echo ""
  echo "Options:"
  echo "  --tag      Create a git tag v<version> after updating files"
  echo "  --dry-run  Show what would change without modifying files"
  echo ""
  echo "Files updated:"
  for f in "${VERSION_FILES[@]}"; do
    echo "  - $f"
  done
  exit 1
}

if [[ $# -lt 1 ]]; then
  usage
fi

VERSION="$1"
shift

if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$ ]]; then
  echo "Error: '$VERSION' is not a valid semver (expected: MAJOR.MINOR.PATCH[-prerelease])" >&2
  exit 1
fi

TAG=false
DRY_RUN=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag) TAG=true; shift ;;
    --dry-run) DRY_RUN=true; shift ;;
    *) echo "Unknown option: $1"; usage ;;
  esac
done

current_version() {
  grep -oP "version = '\K[^']+" "$ROOT_DIR/api/build.gradle"
}

CURRENT="$(current_version)"
echo "Current version: $CURRENT"
echo "New version:     $VERSION"
echo ""

if [[ "$CURRENT" == "$VERSION" ]]; then
  echo "Version is already $VERSION, nothing to do."
  exit 0
fi

update_gradle() {
  local file="$1"
  if $DRY_RUN; then
    echo "  [dry-run] $file: version = '$CURRENT' -> '$VERSION'"
  else
    sed -i "s/version = '.*'/version = '$VERSION'/" "$file"
    echo "  Updated $file"
  fi
}

update_package_json() {
  local file="$1"
  if $DRY_RUN; then
    echo "  [dry-run] $file: \"version\": \"...\" -> \"$VERSION\""
  else
    sed -i "s/\"version\": \".*\"/\"version\": \"$VERSION\"/" "$file"
    echo "  Updated $file"
  fi
}

update_chart_yaml() {
  local file="$1"
  if $DRY_RUN; then
    echo "  [dry-run] $file: version + appVersion -> $VERSION"
  else
    sed -i "s/^version: .*/version: $VERSION/" "$file"
    sed -i "s/^appVersion: .*/appVersion: \"$VERSION\"/" "$file"
    echo "  Updated $file"
  fi
}

echo "Updating files:"
update_gradle "$ROOT_DIR/build.gradle"
update_gradle "$ROOT_DIR/api/build.gradle"
update_package_json "$ROOT_DIR/dashboard/app/package.json"
update_chart_yaml "$ROOT_DIR/charts/ephor/Chart.yaml"
echo ""

if $DRY_RUN; then
  echo "Dry run complete, no files were modified."
  exit 0
fi

if $TAG; then
  TAG_NAME="v$VERSION"
  if git -C "$ROOT_DIR" rev-parse "$TAG_NAME" &>/dev/null; then
    echo "Warning: Tag $TAG_NAME already exists, skipping tag creation."
  else
    git -C "$ROOT_DIR" tag -a "$TAG_NAME" -m "Release $TAG_NAME"
    echo "Created tag: $TAG_NAME"
    echo "Push with: git push origin $TAG_NAME"
  fi
fi

echo "Done. Version bumped to $VERSION."
