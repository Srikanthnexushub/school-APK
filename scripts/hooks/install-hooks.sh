#!/usr/bin/env bash
# Install EduTech git hooks. Run once after cloning or when hooks change.
# Usage: bash scripts/hooks/install-hooks.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOOKS_SRC="${ROOT_DIR}/scripts/hooks"
HOOKS_DST="${ROOT_DIR}/.git/hooks"

for hook in pre-commit post-commit; do
  cp "${HOOKS_SRC}/${hook}" "${HOOKS_DST}/${hook}"
  chmod +x "${HOOKS_DST}/${hook}"
  echo "Installed: .git/hooks/${hook}"
done

echo "All hooks installed."
