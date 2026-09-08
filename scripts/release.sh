#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/release.sh <major|minor|patch>
# Runs bump-version.sh followed by open-release-pr.sh to perform
# a full release in one step.

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <major|minor|patch>" >&2
  exit 1
fi

TYPE="$1"

if [[ "$TYPE" != "major" && "$TYPE" != "minor" && "$TYPE" != "patch" ]]; then
  echo "ERROR: type must be one of: major, minor, patch" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Step 1/2: bump-version.sh $TYPE ==="
"$SCRIPT_DIR/bump-version.sh" "$TYPE"

echo ""
echo "=== Step 2/2: open-release-pr.sh ==="
"$SCRIPT_DIR/open-release-pr.sh"

echo ""
echo "Release complete."