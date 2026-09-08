#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/bump-version.sh <major|minor|patch>

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <major|minor|patch>" >&2
  exit 1
fi

TYPE="$1"

if [[ "$TYPE" != "major" && "$TYPE" != "minor" && "$TYPE" != "patch" ]]; then
  echo "ERROR: type must be one of: major, minor, patch" >&2
  exit 1
fi

command -v gh >/dev/null 2>&1 || { echo "ERROR: GitHub CLI (gh) is not installed." >&2; exit 1; }

if ! gh auth status >/dev/null 2>&1; then
  echo "ERROR: GitHub CLI is not authenticated. Run 'gh auth login'." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "ERROR: Working tree is not clean. Commit or stash your changes first." >&2
  exit 1
fi

VERSION_FILE="version.properties"
if [[ ! -f "$VERSION_FILE" ]]; then
  echo "ERROR: $VERSION_FILE not found. Run this script from the repository root." >&2
  exit 1
fi

echo "Switching to develop and pulling latest changes..."
git checkout develop
git pull origin develop

CURRENT_VERSION_CODE="$(grep '^versionCode=' "$VERSION_FILE" | cut -d'=' -f2)"
CURRENT_VERSION_NAME="$(grep '^versionName=' "$VERSION_FILE" | cut -d'=' -f2)"

if [[ -z "$CURRENT_VERSION_CODE" || -z "$CURRENT_VERSION_NAME" ]]; then
  echo "ERROR: Could not read versionCode/versionName from $VERSION_FILE" >&2
  exit 1
fi

IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION_NAME"

case "$TYPE" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
esac

NEW_VERSION_NAME="${MAJOR}.${MINOR}.${PATCH}"
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

echo "Current version: versionCode=$CURRENT_VERSION_CODE, versionName=$CURRENT_VERSION_NAME"
echo "New version:     versionCode=$NEW_VERSION_CODE, versionName=$NEW_VERSION_NAME"

read -rp "Proceed with this $TYPE bump? (y/N) " CONFIRM
if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
  echo "Aborted."
  exit 0
fi

echo "Creating GitHub issue..."
ISSUE_URL="$(gh issue create \
  --title "Bump version to $NEW_VERSION_NAME" \
  --body "$(cat <<EOF
## Goal
Bump the app version ($TYPE release).

## Tasks
- [ ] Update versionCode to $NEW_VERSION_CODE
- [ ] Update versionName to $NEW_VERSION_NAME
EOF
)")"

ISSUE_NUMBER="${ISSUE_URL##*/}"
echo "Created issue #$ISSUE_NUMBER: $ISSUE_URL"

BRANCH_NAME="chore/${ISSUE_NUMBER}-bump-version-${NEW_VERSION_NAME}"
echo "Creating branch $BRANCH_NAME..."
git checkout -b "$BRANCH_NAME"

sed -i -E \
  -e "s/^versionCode=[0-9]+$/versionCode=${NEW_VERSION_CODE}/" \
  -e "s/^versionName=[0-9]+\.[0-9]+\.[0-9]+$/versionName=${NEW_VERSION_NAME}/" \
  "$VERSION_FILE"

git add "$VERSION_FILE"
git commit -m "chore: bump version to ${NEW_VERSION_NAME}

Closes #${ISSUE_NUMBER}"

git push -u origin "$BRANCH_NAME"

echo "Creating pull request into develop..."
PR_URL="$(gh pr create \
  --base develop \
  --head "$BRANCH_NAME" \
  --title "chore: bump version to ${NEW_VERSION_NAME}" \
  --body "Closes #${ISSUE_NUMBER}")"

PR_NUMBER="${PR_URL##*/}"
echo "Created PR #$PR_NUMBER: $PR_URL"

echo "Waiting for checks to complete..."
if gh pr checks "$PR_NUMBER" --watch; then
  echo "Checks passed. Merging PR #$PR_NUMBER into develop..."
  gh pr merge "$PR_NUMBER" --merge --delete-branch
  echo "Merged. develop now has version ${NEW_VERSION_NAME}."
else
  echo "ERROR: Checks failed for PR #$PR_NUMBER. Not merging." >&2
  echo "Review the PR, push fixes to $BRANCH_NAME, and merge manually once green." >&2
  exit 1
fi