#!/usr/bin/env bash
# =============================================================================
# EduTech AI Platform — Safe cleanup of stale artifacts
# =============================================================================
# Usage:
#   bash scripts/cleanup.sh           # interactive (prompts before big deletes)
#   bash scripts/cleanup.sh --force   # skip confirmation prompts
#   bash scripts/cleanup.sh --logs    # logs only
#   bash scripts/cleanup.sh --build   # Maven target/ dirs only
#   bash scripts/cleanup.sh --all     # everything (requires --force)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOGS_DIR="${ROOT_DIR}/logs"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
section() { echo -e "\n${CYAN}${BOLD}═══ $* ═══${NC}"; }

FORCE=false
DO_LOGS=false
DO_BUILD=false
DO_ALL=false

for arg in "$@"; do
  case $arg in
    --force)  FORCE=true ;;
    --logs)   DO_LOGS=true ;;
    --build)  DO_BUILD=true ;;
    --all)    DO_ALL=true ;;
  esac
done

# Default: logs only
if ! $DO_LOGS && ! $DO_BUILD && ! $DO_ALL; then
  DO_LOGS=true
fi
if $DO_ALL; then
  DO_LOGS=true
  DO_BUILD=true
fi

confirm() {
  local msg="$1"
  if $FORCE; then return 0; fi
  echo -en "${YELLOW}[?]${NC}    ${msg} [y/N] "
  read -r reply
  [[ "${reply:-n}" =~ ^[Yy]$ ]]
}

freed=0

# ─── 1. Logs ─────────────────────────────────────────────────────────────────
if $DO_LOGS && [[ -d "${LOGS_DIR}" ]]; then
  section "Log files"
  total_logs=$(du -sh "${LOGS_DIR}" 2>/dev/null | cut -f1)
  info "Current logs/ usage: ${total_logs}"

  # Truncate active logs (preserves file handles for running services)
  for log_file in "${LOGS_DIR}"/*.log; do
    [[ -f "${log_file}" ]] || continue
    size=$(wc -c < "${log_file}" 2>/dev/null || echo 0)
    if [[ ${size} -gt 0 ]]; then
      mb=$(( size / 1024 / 1024 ))
      warn "  $(basename "${log_file}"): ${mb} MB — truncating (services keep running)"
      : > "${log_file}"
    fi
  done

  # Delete rotated backups
  find "${LOGS_DIR}" -name "*.log.*" -delete 2>/dev/null || true
  find "${LOGS_DIR}" -name "*.watchdog.*" -delete 2>/dev/null || true

  after_logs=$(du -sh "${LOGS_DIR}" 2>/dev/null | cut -f1)
  info "After cleanup: ${after_logs}"
fi

# ─── 2. Playwright/MCP artifacts ─────────────────────────────────────────────
if [[ -d "${ROOT_DIR}/.playwright-mcp" ]]; then
  section "Playwright MCP artifacts"
  size=$(du -sh "${ROOT_DIR}/.playwright-mcp" 2>/dev/null | cut -f1)
  info ".playwright-mcp usage: ${size}"
  if confirm "Delete .playwright-mcp/ contents?"; then
    find "${ROOT_DIR}/.playwright-mcp" -name "*.log" -delete 2>/dev/null || true
    find "${ROOT_DIR}/.playwright-mcp" -name "*.png" -delete 2>/dev/null || true
    info "Cleared .playwright-mcp/ artifacts."
  fi
fi

# ─── 3. Screenshot PNGs at project root ──────────────────────────────────────
section "Root-level screenshots"
png_count=$(find "${ROOT_DIR}" -maxdepth 1 -name "*.png" -o -name "*.jpg" 2>/dev/null | wc -l | tr -d ' ')
if [[ ${png_count} -gt 0 ]]; then
  size=$(find "${ROOT_DIR}" -maxdepth 1 \( -name "*.png" -o -name "*.jpg" \) -exec du -sh {} + 2>/dev/null | tail -1 | cut -f1 || echo "?")
  warn "Found ${png_count} screenshot file(s) at project root."
  if confirm "Delete ${png_count} screenshot file(s)?"; then
    find "${ROOT_DIR}" -maxdepth 1 \( -name "*.png" -o -name "*.jpg" \) -delete
    info "Deleted ${png_count} screenshot file(s)."
  fi
else
  info "No screenshots at project root. ✓"
fi

# ─── 4. Maven target/ directories ────────────────────────────────────────────
if $DO_BUILD; then
  section "Maven build artifacts (target/)"
  target_size=$(find "${ROOT_DIR}/services" -name "target" -type d -maxdepth 3 \
    -exec du -sh {} \; 2>/dev/null | awk '{sum += $1} END {print sum}' || echo "?")
  info "Total target/ usage: ~${target_size} blocks"
  if confirm "Delete all services/*/target/ directories? (requires rebuild with mvn clean package)"; then
    find "${ROOT_DIR}/services" -name "target" -maxdepth 3 -type d -exec rm -rf {} + 2>/dev/null || true
    info "Deleted target/ directories."
  fi
fi

# ─── 5. Maven local cache for this project ───────────────────────────────────
if $DO_ALL; then
  section "Maven local cache (com.edutech snapshots)"
  edutech_cache="${HOME}/.m2/repository/com/edutech"
  if [[ -d "${edutech_cache}" ]]; then
    size=$(du -sh "${edutech_cache}" 2>/dev/null | cut -f1)
    warn "com.edutech cache: ${size}"
    if confirm "Delete ~/.m2/repository/com/edutech? (will be rebuilt on next mvn package)"; then
      rm -rf "${edutech_cache}"
      info "Cleared Maven cache."
    fi
  else
    info "No com.edutech Maven cache found. ✓"
  fi
fi

# ─── Summary ─────────────────────────────────────────────────────────────────
section "Disk summary"
df -h / | tail -1 | awk '{printf "  Disk: %s used of %s (%s free)\n", $3, $2, $4}'
echo ""
info "Cleanup complete."
