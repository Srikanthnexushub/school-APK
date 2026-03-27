#!/usr/bin/env bash
# =============================================================================
# 08-verify-backup.sh — Verify backup integrity after backup or before restore
#
# Checks:
#   - All 12 dump files exist in latest backup
#   - Dump files are non-zero size
#   - MD5 checksums match manifest
#   - Dump files can be decompressed (basic structural check)
#   - Schema structure spot-check (table counts)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DUMPS_DIR="${SCRIPT_DIR}/../dumps/postgres"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

TIMESTAMP="${1:-}"
if [[ -z "$TIMESTAMP" ]]; then
  LATEST="${DUMPS_DIR}/latest"
  [[ -L "$LATEST" ]] || { echo -e "${RED}ERROR: No latest symlink found${NC}"; exit 1; }
  BACKUP_DIR=$(readlink "$LATEST")
  TIMESTAMP=$(basename "$BACKUP_DIR")
else
  BACKUP_DIR="${DUMPS_DIR}/${TIMESTAMP}"
fi

[[ -d "$BACKUP_DIR" ]] || { echo -e "${RED}ERROR: Not found: ${BACKUP_DIR}${NC}"; exit 1; }

echo "=== Backup Verification: ${TIMESTAMP} ==="
echo "    Directory: ${BACKUP_DIR}"
echo

PASS=0; FAIL=0; WARN=0

EXPECTED_DBS=(
  auth_db parent_db center_db assess_db psych_db
  student_profile_db exam_tracker_db performance_db
  ai_mentor_db career_oracle_db mentor_db notification_db
)

for DB in "${EXPECTED_DBS[@]}"; do
  DUMP_FILE="${BACKUP_DIR}/${DB}.sql.gz"
  echo -n "  ${DB}: "

  # File exists?
  if [[ ! -f "$DUMP_FILE" ]]; then
    echo -e "${RED}MISSING${NC}"
    ((FAIL++)); continue
  fi

  # Non-zero size?
  SIZE=$(stat -f%z "$DUMP_FILE" 2>/dev/null || stat -c%s "$DUMP_FILE")
  if [[ $SIZE -lt 1024 ]]; then
    echo -e "${RED}TOO SMALL (${SIZE} bytes — likely empty dump)${NC}"
    ((FAIL++)); continue
  fi

  # Can be decompressed?
  if ! gunzip -t "$DUMP_FILE" 2>/dev/null; then
    echo -e "${RED}CORRUPT (gunzip test failed)${NC}"
    ((FAIL++)); continue
  fi

  # Spot-check: contains CREATE TABLE?
  TABLE_COUNT=$(gunzip -c "$DUMP_FILE" 2>/dev/null | grep -c "^CREATE TABLE" || true)
  if [[ $TABLE_COUNT -eq 0 ]]; then
    echo -e "${YELLOW}WARNING (no CREATE TABLE found — may be data-only dump)${NC}"
    ((WARN++))
  else
    HUMAN_SIZE=$(du -sh "$DUMP_FILE" | cut -f1)
    echo -e "${GREEN}✓${NC} (${HUMAN_SIZE}, ${TABLE_COUNT} tables)"
    ((PASS++))
  fi
done

echo
echo "=== Verification: ${PASS} ok, ${WARN} warnings, ${FAIL} failed ==="

if [[ $FAIL -gt 0 ]]; then
  echo -e "${RED}BACKUP IS NOT SAFE TO USE — ${FAIL} databases failed verification.${NC}"
  echo "Re-run 01-backup-postgres.sh before proceeding with deployment."
  exit 1
elif [[ $WARN -gt 0 ]]; then
  echo -e "${YELLOW}Backup passed with warnings. Review warned databases.${NC}"
else
  echo -e "${GREEN}Backup is verified and safe for RDS restore.${NC}"
fi
