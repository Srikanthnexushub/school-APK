#!/usr/bin/env bash
# =============================================================================
# 05-backup-all.sh — Master backup script (runs all 4 backup scripts)
#
# Usage:
#   bash 05-backup-all.sh              # full backup
#   bash 05-backup-all.sh --skip-minio # skip MinIO (faster)
#   bash 05-backup-all.sh --skip-redis # skip Redis
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SKIP_MINIO=false; SKIP_REDIS=false
for arg in "$@"; do
  [[ "$arg" == "--skip-minio" ]] && SKIP_MINIO=true
  [[ "$arg" == "--skip-redis" ]] && SKIP_REDIS=true
done

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
OVERALL_START=$(date +%s)
ERRORS=()

run_step() {
  local step_num="$1"; local step_name="$2"; local script="$3"
  echo
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  Step ${step_num}: ${step_name}"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  if bash "${SCRIPT_DIR}/${script}"; then
    echo -e "  ${GREEN}✓ ${step_name} completed${NC}"
  else
    echo -e "  ${RED}✗ ${step_name} FAILED${NC}"
    ERRORS+=("$step_name")
  fi
}

echo "╔═══════════════════════════════════════════════╗"
echo "║     EduTech Platform — Full Data Backup       ║"
echo "║     $(date '+%Y-%m-%d %H:%M:%S')                   ║"
echo "╚═══════════════════════════════════════════════╝"

# Step 0: Pre-flight
run_step 0 "Pre-flight checks" "00-verify-prereqs.sh"

# Step 1: PostgreSQL (most important)
run_step 1 "PostgreSQL backup (12 databases)" "01-backup-postgres.sh"

# Step 2: Redis (optional)
if [[ "$SKIP_REDIS" == "true" ]]; then
  echo -e "\n  ${YELLOW}⚠ Skipping Redis backup (--skip-redis)${NC}"
else
  run_step 2 "Redis backup" "02-backup-redis.sh"
fi

# Step 3: MinIO (optional, can be large)
if [[ "$SKIP_MINIO" == "true" ]]; then
  echo -e "\n  ${YELLOW}⚠ Skipping MinIO backup (--skip-minio)${NC}"
else
  run_step 3 "MinIO object storage backup" "03-backup-minio.sh"
fi

# Step 4: JWT keys + config
run_step 4 "JWT keys + configuration" "04-backup-keys.sh"

# Summary
ELAPSED=$(( $(date +%s) - OVERALL_START ))
echo
echo "╔═══════════════════════════════════════════════╗"
echo "║              Backup Summary                   ║"
echo "╠═══════════════════════════════════════════════╣"
echo "║  Duration: ${ELAPSED}s"
echo "║  Output:   data-backup/dumps/"
if [[ ${#ERRORS[@]} -eq 0 ]]; then
  echo -e "║  ${GREEN}Status: ALL SUCCEEDED${NC}"
  echo "╚═══════════════════════════════════════════════╝"
  echo
  echo "Next steps:"
  echo "  1. bash 06-restore-to-rds.sh          — restore Postgres dumps to AWS RDS"
  echo "  2. bash 07-export-minio-to-s3.sh       — upload MinIO dumps to AWS S3"
  echo "  3. Upload keys/jwt-private.pem to AWS Secrets Manager"
  exit 0
else
  echo -e "║  ${RED}Status: FAILED steps: ${ERRORS[*]}${NC}"
  echo "╚═══════════════════════════════════════════════╝"
  exit 1
fi
