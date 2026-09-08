#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/open-release-pr.sh
# Run this AFTER the version bump PR (bump-version.sh) has been merged into develop.

command -v gh >/dev/null 2>&1 || { echo "ERROR: GitHub CLI (gh) is not installed." >&2; exit 1; }

if ! gh auth status >/dev/null 2>&1; then
  echo "ERROR: GitHub CLI is not authenticated. Run 'gh auth login'." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "ERROR: Working tree is not clean. Commit or stash your changes first." >&2
  exit 1
fi

echo "Switching to develop and pulling latest changes..."
git checkout develop
git pull origin develop

VERSION_FILE="version.properties"
if [[ ! -f "$VERSION_FILE" ]]; then
  echo "ERROR: $VERSION_FILE not found. Run this script from the repository root." >&2
  exit 1
fi

VERSION_NAME="$(grep '^versionName=' "$VERSION_FILE" | cut -d'=' -f2)"

if [[ -z "$VERSION_NAME" ]]; then
  echo "ERROR: Could not read versionName from $VERSION_FILE" >&2
  exit 1
fi

echo "Preparing release PR for version $VERSION_NAME..."
read -rp "Open PR 'Release $VERSION_NAME' from develop into main? (y/N) " CONFIRM
if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
  echo "Aborted."
  exit 0
fi

PR_URL="$(gh pr create \
  --base main \
  --head develop \
  --title "Release $VERSION_NAME" \
  --body "Automated release PR for version $VERSION_NAME.")"

PR_NUMBER="${PR_URL##*/}"
echo "Created release PR #$PR_NUMBER: $PR_URL"

echo "Waiting for checks to complete (validate + validate-release)..."
if gh pr checks "$PR_NUMBER" --watch; then
  echo "Checks passed. Merging PR #$PR_NUMBER into main..."
  gh pr merge "$PR_NUMBER" --merge
  echo "Merged. release.yml should now be building, signing and publishing v${VERSION_NAME}."
else
  echo "ERROR: Checks failed for PR #$PR_NUMBER. Not merging." >&2
  echo "Fix the issue on develop, push, and re-run this script." >&2
  exit 1
fi