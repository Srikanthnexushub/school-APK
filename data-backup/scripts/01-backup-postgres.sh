#!/usr/bin/env bash
# =============================================================================
# 01-backup-postgres.sh — pg_dump all 12 EduTech databases
#
# Output: data-backup/dumps/postgres/<timestamp>/
#   - <db_name>.sql.gz         → schema + data (plain SQL, gzip-compressed)
#   - <db_name>-schema-only.sql → schema only (for RDS import validation)
#   - manifest.txt             → checksums + sizes
#
# Usage:
#   bash 01-backup-postgres.sh                  # backup all DBs
#   bash 01-backup-postgres.sh auth_db          # backup single DB
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
DUMPS_DIR="${SCRIPT_DIR}/../dumps/postgres"

# Locate pg_dump — Homebrew PostgreSQL 16 on macOS
PG_BIN=""
for P in /usr/local/opt/postgresql@16/bin /opt/homebrew/opt/postgresql@16/bin /usr/lib/postgresql/16/bin /usr/bin; do
  [[ -x "${P}/pg_dump" ]] && PG_BIN="$P" && break
done
[[ -z "$PG_BIN" ]] && { echo "ERROR: pg_dump not found — install: brew install postgresql@16"; exit 1; }
export PATH="${PG_BIN}:${PATH}"

# Source environment
[[ -f "$ENV_FILE" ]] || { echo "ERROR: .env not found at $ENV_FILE"; exit 1; }
source "$ENV_FILE"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${DUMPS_DIR}/${TIMESTAMP}"
mkdir -p "$BACKUP_DIR"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

# Format: "db_name:username:password"
ALL_DBS=(
  "auth_db:${AUTH_SVC_DB_USERNAME}:${AUTH_SVC_DB_PASSWORD}"
  "parent_db:${PARENT_SVC_DB_USERNAME}:${PARENT_SVC_DB_PASSWORD}"
  "center_db:${CENTER_SVC_DB_USERNAME}:${CENTER_SVC_DB_PASSWORD}"
  "assess_db:${ASSESS_SVC_DB_USERNAME}:${ASSESS_SVC_DB_PASSWORD}"
  "psych_db:${PSYCH_SVC_DB_USERNAME}:${PSYCH_SVC_DB_PASSWORD}"
  "student_profile_db:${STUDENT_PROFILE_SVC_DB_USERNAME}:${STUDENT_PROFILE_SVC_DB_PASSWORD}"
  "exam_tracker_db:${EXAM_TRACKER_SVC_DB_USERNAME}:${EXAM_TRACKER_SVC_DB_PASSWORD}"
  "performance_db:${PERFORMANCE_SVC_DB_USERNAME}:${PERFORMANCE_SVC_DB_PASSWORD}"
  "ai_mentor_db:${AI_MENTOR_DB_USER}:${AI_MENTOR_DB_PASSWORD}"
  "career_oracle_db:${CAREER_ORACLE_DB_USER}:${CAREER_ORACLE_DB_PASSWORD}"
  "mentor_db:${MENTOR_DB_USER}:${MENTOR_DB_PASSWORD}"
  "notification_db:${NOTIFICATION_SVC_DB_USERNAME}:${NOTIFICATION_SVC_DB_PASSWORD}"
)

# Filter to single DB if argument provided
TARGET="${1:-}"
if [[ -n "$TARGET" ]]; then
  FILTERED=()
  for entry in "${ALL_DBS[@]}"; do
    [[ "$entry" == "${TARGET}:"* ]] && FILTERED+=("$entry")
  done
  if [[ ${#FILTERED[@]} -eq 0 ]]; then
    echo -e "${RED}ERROR: DB '${TARGET}' not found in list${NC}"
    exit 1
  fi
  ALL_DBS=("${FILTERED[@]}")
fi

PASS=0; FAIL=0
MANIFEST="${BACKUP_DIR}/manifest.txt"
echo "# EduTech PostgreSQL Backup Manifest" > "$MANIFEST"
echo "# Timestamp: ${TIMESTAMP}" >> "$MANIFEST"
echo "# Host: ${POSTGRES_HOST}:${POSTGRES_PORT}" >> "$MANIFEST"
echo "" >> "$MANIFEST"

# Use SUPERUSER for all dumps — required because:
# 1. auth_db and center_db have RLS (Row-Level Security) on users/centers tables
#    — service users cannot dump RLS-protected rows; superuser bypasses RLS
# 2. psych_db has large objects — service user lacks pg_largeobject permission
# The superuser sees all data and bypasses all RLS policies.
ROOT_USER="${POSTGRES_ROOT_USER}"
ROOT_PASS="${POSTGRES_ROOT_PASSWORD}"

echo "=== PostgreSQL Backup [${TIMESTAMP}] ==="
echo "   Host: ${POSTGRES_HOST}:${POSTGRES_PORT}"
echo "   User: ${ROOT_USER} (superuser — bypasses RLS + large objects)"
echo "   Output: ${BACKUP_DIR}"
echo

for entry in "${ALL_DBS[@]}"; do
  IFS=':' read -r DB USER PASS_VAL <<< "$entry"

  echo -n "  Backing up ${DB}... "

  # Full data dump using superuser (gzip compressed)
  DUMP_FILE="${BACKUP_DIR}/${DB}.sql.gz"
  if PGPASSWORD="$ROOT_PASS" pg_dump \
      -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" \
      -U "$ROOT_USER" \
      -d "$DB" \
      --verbose \
      --no-password \
      --format=plain \
      --no-owner \
      --no-acl \
      2>>"${BACKUP_DIR}/${DB}.log" | gzip -9 > "$DUMP_FILE"; then

    # Schema-only dump (plain, for validation)
    SCHEMA_FILE="${BACKUP_DIR}/${DB}-schema-only.sql"
    PGPASSWORD="$ROOT_PASS" pg_dump \
      -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" \
      -U "$ROOT_USER" \
      -d "$DB" \
      --schema-only \
      --no-password \
      --no-owner \
      --no-acl \
      2>/dev/null > "$SCHEMA_FILE"

    SIZE=$(du -sh "$DUMP_FILE" | cut -f1)
    CHECKSUM=$(md5 -q "$DUMP_FILE" 2>/dev/null || md5sum "$DUMP_FILE" | cut -d' ' -f1)
    echo -e "${GREEN}✓${NC} (${SIZE})"
    echo "${DB}.sql.gz  size=${SIZE}  md5=${CHECKSUM}" >> "$MANIFEST"
    ((PASS++))
  else
    echo -e "${RED}✗ FAILED${NC} — see ${BACKUP_DIR}/${DB}.log"
    echo "${DB}.sql.gz  FAILED" >> "$MANIFEST"
    ((FAIL++))
  fi
done

echo
echo "=== Result: ${PASS} succeeded, ${FAIL} failed ==="
echo "    Backup stored in: ${BACKUP_DIR}"
echo "    Manifest: ${MANIFEST}"

# Keep symlink to latest
LATEST_LINK="${DUMPS_DIR}/latest"
rm -f "$LATEST_LINK"
ln -s "$BACKUP_DIR" "$LATEST_LINK"
echo "    Latest symlink: ${LATEST_LINK} → ${BACKUP_DIR}"

[[ $FAIL -gt 0 ]] && exit 1 || exit 0
