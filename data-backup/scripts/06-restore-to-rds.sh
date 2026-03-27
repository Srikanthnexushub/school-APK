#!/usr/bin/env bash
# =============================================================================
# 06-restore-to-rds.sh — Restore pg_dump backups to AWS RDS PostgreSQL
#
# Prerequisites:
#   - RDS instance must be RUNNING and ACCESSIBLE
#   - All 12 databases + users must be PRE-CREATED (see Phase 2, Step 2 in guide)
#   - pgvector extension must be enabled in assess_db
#   - pg_restore / psql must be installed locally
#   - VPN or SSH tunnel to RDS required (RDS is NOT publicly accessible)
#
# Usage:
#   export RDS_HOST=<your-rds-endpoint>
#   export RDS_MASTER_USER=postgres
#   export RDS_MASTER_PASSWORD=<your-rds-password>
#   bash 06-restore-to-rds.sh                        # restore latest backup
#   bash 06-restore-to-rds.sh 20260328_120000         # restore specific timestamp
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
DUMPS_DIR="${SCRIPT_DIR}/../dumps/postgres"

[[ -f "$ENV_FILE" ]] || { echo "ERROR: .env not found"; exit 1; }
source "$ENV_FILE"

# Locate psql
for P in /usr/local/opt/postgresql@16/bin /opt/homebrew/opt/postgresql@16/bin /usr/lib/postgresql/16/bin /usr/bin; do
  [[ -x "${P}/psql" ]] && export PATH="${P}:${PATH}" && break
done

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

# ── RDS Connection Config ─────────────────────────────────────────────────────
RDS_HOST="${RDS_HOST:-}"
RDS_PORT="${RDS_PORT:-5432}"
RDS_MASTER_USER="${RDS_MASTER_USER:-postgres}"
RDS_MASTER_PASSWORD="${RDS_MASTER_PASSWORD:-}"

if [[ -z "$RDS_HOST" || -z "$RDS_MASTER_PASSWORD" ]]; then
  echo -e "${RED}ERROR: Set RDS_HOST and RDS_MASTER_PASSWORD environment variables before running.${NC}"
  echo "  export RDS_HOST=your-rds-endpoint.rds.amazonaws.com"
  echo "  export RDS_MASTER_USER=postgres"
  echo "  export RDS_MASTER_PASSWORD=your-rds-master-password"
  exit 1
fi

# ── Select backup to restore ──────────────────────────────────────────────────
TIMESTAMP="${1:-}"
if [[ -z "$TIMESTAMP" ]]; then
  LATEST="${DUMPS_DIR}/latest"
  if [[ -L "$LATEST" ]]; then
    BACKUP_DIR=$(readlink "$LATEST")
    TIMESTAMP=$(basename "$BACKUP_DIR")
  else
    echo -e "${RED}ERROR: No 'latest' symlink found. Run 01-backup-postgres.sh first.${NC}"
    exit 1
  fi
else
  BACKUP_DIR="${DUMPS_DIR}/${TIMESTAMP}"
fi

[[ -d "$BACKUP_DIR" ]] || { echo -e "${RED}ERROR: Backup dir not found: ${BACKUP_DIR}${NC}"; exit 1; }

echo "╔═══════════════════════════════════════════════╗"
echo "║     Restore PostgreSQL → AWS RDS              ║"
echo "╠═══════════════════════════════════════════════╣"
echo "║  Source backup: ${TIMESTAMP}"
echo "║  RDS Host:      ${RDS_HOST}:${RDS_PORT}"
echo "╚═══════════════════════════════════════════════╝"
echo
echo -e "${YELLOW}⚠ This will OVERWRITE data in RDS databases.${NC}"
echo -n "  Type 'yes' to continue: "
read -r CONFIRM
[[ "$CONFIRM" == "yes" ]] || { echo "Aborted."; exit 0; }

# ── Restore each database ─────────────────────────────────────────────────────
# Map: source DB name → RDS target DB name (use SAME names unless migrating)
declare -A DB_USERS=(
  [auth_db]="${AUTH_SVC_DB_USERNAME}"
  [parent_db]="${PARENT_SVC_DB_USERNAME}"
  [center_db]="${CENTER_SVC_DB_USERNAME}"
  [assess_db]="${ASSESS_SVC_DB_USERNAME}"
  [psych_db]="${PSYCH_SVC_DB_USERNAME}"
  [student_profile_db]="${STUDENT_PROFILE_SVC_DB_USERNAME}"
  [exam_tracker_db]="${EXAM_TRACKER_SVC_DB_USERNAME}"
  [performance_db]="${PERFORMANCE_SVC_DB_USERNAME}"
  [ai_mentor_db]="${AI_MENTOR_DB_USER}"
  [career_oracle_db]="${CAREER_ORACLE_DB_USER}"
  [mentor_db]="${MENTOR_DB_USER}"
  [notification_db]="${NOTIFICATION_SVC_DB_USERNAME}"
)

PASS=0; FAIL=0

for DB in "${!DB_USERS[@]}"; do
  DUMP_FILE="${BACKUP_DIR}/${DB}.sql.gz"
  if [[ ! -f "$DUMP_FILE" ]]; then
    echo -e "  ${YELLOW}⚠${NC} Skipping ${DB} — dump file not found"
    continue
  fi

  echo -n "  Restoring ${DB}... "

  # Decompress and pipe to psql
  if gunzip -c "$DUMP_FILE" | PGPASSWORD="$RDS_MASTER_PASSWORD" psql \
    -h "$RDS_HOST" -p "$RDS_PORT" \
    -U "$RDS_MASTER_USER" \
    -d "$DB" \
    --single-transaction \
    -v ON_ERROR_STOP=1 \
    2>"${BACKUP_DIR}/${DB}-rds-restore.log"; then
    echo -e "${GREEN}✓${NC}"
    ((PASS++))
  else
    echo -e "${RED}✗ FAILED${NC} — see ${BACKUP_DIR}/${DB}-rds-restore.log"
    ((FAIL++))
  fi
done

echo
echo "=== Restore result: ${PASS} succeeded, ${FAIL} failed ==="

if [[ $FAIL -gt 0 ]]; then
  echo -e "${RED}Some databases failed to restore. Review logs in ${BACKUP_DIR}/${NC}"
  exit 1
else
  echo -e "${GREEN}All databases restored successfully to RDS.${NC}"
  echo
  echo "Post-restore verification:"
  echo "  psql -h ${RDS_HOST} -U postgres -c '\l'"
  echo "  psql -h ${RDS_HOST} -U postgres -d auth_db -c 'SELECT count(*) FROM auth_schema.users;'"
fi
