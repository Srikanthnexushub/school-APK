#!/usr/bin/env bash
# =============================================================================
# 00-verify-prereqs.sh — Verify all tools and connections before backup
# Run this FIRST before any backup operation.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
PASS=0; FAIL=0

check() {
  local label="$1"; local result="$2"
  if [[ "$result" == "ok" ]]; then
    echo -e "  ${GREEN}✓${NC} ${label}"
    ((PASS++))
  else
    echo -e "  ${RED}✗${NC} ${label}: ${result}"
    ((FAIL++))
  fi
}

# Locate Homebrew PostgreSQL binaries
for P in /usr/local/opt/postgresql@16/bin /opt/homebrew/opt/postgresql@16/bin /usr/lib/postgresql/16/bin /usr/bin; do
  [[ -x "${P}/pg_dump" ]] && export PATH="${P}:${PATH}" && break
done

echo "=== EduTech Backup Pre-flight Check ==="
echo

# ── 1. .env file ─────────────────────────────────────────────────────────────
echo "[ Required Files ]"
[[ -f "$ENV_FILE" ]] && check ".env file exists" "ok" || check ".env file exists" "NOT FOUND at $ENV_FILE"
source "$ENV_FILE" 2>/dev/null || { echo -e "${RED}FATAL: Cannot source .env${NC}"; exit 1; }

# ── 2. Tools ──────────────────────────────────────────────────────────────────
echo
echo "[ Required Tools ]"
for tool in pg_dump psql redis-cli mc aws; do
  command -v "$tool" &>/dev/null && check "$tool" "ok" || check "$tool" "NOT INSTALLED"
done

# ── 3. PostgreSQL connectivity ────────────────────────────────────────────────
echo
echo "[ PostgreSQL Connectivity ]"
DBS=(
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

for entry in "${DBS[@]}"; do
  IFS=':' read -r db user pass <<< "$entry"
  if PGPASSWORD="$pass" psql -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "$user" -d "$db" -c "SELECT 1" &>/dev/null; then
    check "$db ($user)" "ok"
  else
    check "$db ($user)" "CONNECTION FAILED"
  fi
done

# ── 4. Redis connectivity ─────────────────────────────────────────────────────
echo
echo "[ Redis ]"
if redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" PING 2>/dev/null | grep -q PONG; then
  check "Redis connection" "ok"
else
  check "Redis connection" "PING FAILED"
fi

# ── 5. MinIO connectivity ─────────────────────────────────────────────────────
echo
echo "[ MinIO ]"
if mc alias set backup-minio "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" &>/dev/null; then
  check "MinIO alias set" "ok"
  mc ls "backup-minio/${MINIO_BUCKET_NAME}" &>/dev/null && check "Bucket ${MINIO_BUCKET_NAME}" "ok" || check "Bucket ${MINIO_BUCKET_NAME}" "NOT FOUND"
else
  check "MinIO connection" "FAILED"
fi

# ── 6. Disk space ─────────────────────────────────────────────────────────────
echo
echo "[ Disk Space ]"
AVAIL_MB=$(df -m "${SCRIPT_DIR}/../dumps" | awk 'NR==2 {print $4}')
if [[ $AVAIL_MB -gt 2048 ]]; then
  check "Available space: ${AVAIL_MB} MB" "ok"
else
  check "Available space: ${AVAIL_MB} MB (< 2 GB warning)" "LOW DISK"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo
echo "=== Result: ${PASS} passed, ${FAIL} failed ==="
if [[ $FAIL -gt 0 ]]; then
  echo -e "${RED}Fix failures above before running backup scripts.${NC}"
  exit 1
else
  echo -e "${GREEN}All checks passed. Safe to run backup.${NC}"
fi
