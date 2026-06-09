#!/usr/bin/env bash
# scripts/release.sh — local release helper for SapienTerm
#
# Usage:
#   ./scripts/release.sh v1.2.3
#
# Prerequisites:
#   - Doppler CLI installed and authenticated (doppler login)
#   - Doppler project "apps", config "android" has SAPIENTERM_SECRETS populated
#   - Git working tree is clean and on the branch you want to tag
#
# What this script does:
#   1. Pulls signing secrets from Doppler at runtime (nothing stored locally)
#   2. Writes a temporary keystore from the base64 secret
#   3. Builds a signed googleRelease AAB
#   4. Creates and pushes the git tag → triggers the GitHub Actions release workflow
#   5. Cleans up the temporary keystore

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

# ── pull secrets from Doppler ─────────────────────────────────────────────────
bold "==> Fetching signing secrets from Doppler (apps/android)…"
command -v doppler >/dev/null 2>&1 || die "doppler CLI not found. Install from https://docs.doppler.com/docs/cli"
command -v jq      >/dev/null 2>&1 || die "jq not found. Install with: brew install jq"

SECRETS_JSON="$(doppler secrets get SAPIENTERM_SECRETS --project apps --config android --plain)"

KEYSTORE_B64="$(echo "$SECRETS_JSON"   | jq -r '.keystore_base64')"
KEYSTORE_PASSWORD="$(echo "$SECRETS_JSON" | jq -r '.keystore_password')"
KEY_ALIAS="$(echo "$SECRETS_JSON"      | jq -r '.key_alias')"
KEY_PASSWORD="$(echo "$SECRETS_JSON"   | jq -r '.key_password')"

# ── write temporary keystore ──────────────────────────────────────────────────
KEYSTORE_PATH="$(mktemp /tmp/sapienterm-release-XXXXXX.jks)"
trap 'rm -f "$KEYSTORE_PATH"' EXIT
echo "$KEYSTORE_B64" | base64 --decode > "$KEYSTORE_PATH"
green "✓ Keystore written to temp file (cleaned up on exit)"

# ── build ─────────────────────────────────────────────────────────────────────
bold "==> Building signed release AAB (bundleGoogleRelease)…"
export KEYSTORE_PATH KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD
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
echo "  2. Once complete, check the Play Store internal track:"
echo "     https://play.google.com/console"
echo "  3. Promote from internal → production when ready."
echo ""
green "Done!"
