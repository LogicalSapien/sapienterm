#!/usr/bin/env bash
# scripts/release.sh — local release helper for SapienTerm
#
# Usage:
#   ./scripts/release.sh v1.2.3
#
# Prerequisites:
#   - KEYSTORE_PATH (or keystoreFile Gradle property) pointing to a valid .jks file
#   - KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD env vars set
#     OR the corresponding keystorePassword / keystoreAlias Gradle properties in
#     ~/.gradle/gradle.properties
#   - Git working tree is clean and on the branch you want to tag
#   - Google Play credentials configured if you want to publish manually
#
# What this script does:
#   1. Validates inputs and keystore presence
#   2. Builds a signed googleRelease AAB
#   3. Creates and pushes the git tag  →  triggers the GitHub Actions release workflow

set -euo pipefail

# ── helpers ──────────────────────────────────────────────────────────────────
red()    { printf '\033[0;31m%s\033[0m\n' "$*"; }
green()  { printf '\033[0;32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[0;33m%s\033[0m\n' "$*"; }
bold()   { printf '\033[1m%s\033[0m\n' "$*"; }
die()    { red "ERROR: $*"; exit 1; }

# ── argument check ────────────────────────────────────────────────────────────
[[ $# -eq 1 ]] || die "Usage: $0 <version-tag>  e.g.  $0 v1.2.3"

VERSION="$1"

# Validate semver tag format
[[ "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] \
  || die "Version tag must match v<major>.<minor>.<patch> (got: $VERSION)"

# ── repo root ─────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

bold "==> SapienTerm release: $VERSION"

# ── clean working tree check ──────────────────────────────────────────────────
if [[ -n "$(git status --porcelain)" ]]; then
  yellow "WARNING: Working tree has uncommitted changes:"
  git status --short
  read -r -p "Continue anyway? [y/N] " confirm
  [[ "$confirm" =~ ^[Yy]$ ]] || die "Aborted — commit or stash your changes first."
fi

# ── tag collision check ───────────────────────────────────────────────────────
if git rev-parse "$VERSION" >/dev/null 2>&1; then
  die "Tag $VERSION already exists locally. Delete it first: git tag -d $VERSION"
fi

# ── keystore check ────────────────────────────────────────────────────────────
# Resolve keystore path: env var → Gradle property
KEYSTORE_PATH="${KEYSTORE_PATH:-}"
if [[ -z "$KEYSTORE_PATH" ]]; then
  GRADLE_PROPS="$HOME/.gradle/gradle.properties"
  if [[ -f "$GRADLE_PROPS" ]]; then
    KEYSTORE_PATH="$(grep -E '^keystoreFile\s*=' "$GRADLE_PROPS" | sed 's/^keystoreFile\s*=\s*//' | tr -d '[:space:]')" || true
  fi
fi

[[ -n "$KEYSTORE_PATH" ]] \
  || die "No keystore found. Set KEYSTORE_PATH env var or keystoreFile in ~/.gradle/gradle.properties"
[[ -f "$KEYSTORE_PATH" ]] \
  || die "Keystore file not found at: $KEYSTORE_PATH"

green "✓ Keystore found: $KEYSTORE_PATH"

# ── build ─────────────────────────────────────────────────────────────────────
bold "==> Building signed release AAB (bundleGoogleRelease)…"
export KEYSTORE_PATH
./gradlew bundleGoogleRelease --no-daemon

AAB_PATTERN="app/build/outputs/bundle/googleRelease/*.aab"
# shellcheck disable=SC2086
AAB_FILE="$(ls $AAB_PATTERN 2>/dev/null | head -1)"
[[ -n "$AAB_FILE" ]] || die "Build succeeded but no AAB found matching: $AAB_PATTERN"
green "✓ AAB built: $AAB_FILE"

# ── tag & push ────────────────────────────────────────────────────────────────
bold "==> Creating and pushing git tag $VERSION…"
git tag "$VERSION"
git push origin "$VERSION"
green "✓ Tag $VERSION pushed — GitHub Actions release workflow triggered."

# ── next steps ───────────────────────────────────────────────────────────────
echo ""
bold "Next steps:"
echo "  1. Monitor the Actions run:"
echo "     https://github.com/$(git remote get-url origin | sed 's|.*github.com[:/]\(.*\)\.git|\1|')/actions"
echo "  2. Once the workflow completes, check the Play Store internal track:"
echo "     https://play.google.com/console"
echo "  3. Promote from internal → production when ready."
echo ""
green "Done! 🚀"
