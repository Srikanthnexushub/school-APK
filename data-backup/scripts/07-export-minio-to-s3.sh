#!/usr/bin/env bash
# =============================================================================
# 07-export-minio-to-s3.sh — Migrate MinIO objects to AWS S3
#
# Two modes:
#   1. LIVE MIRROR: stream objects directly from local MinIO → S3 (no local download)
#   2. FROM BACKUP: upload a previously created minio_backup_*.tar.gz to S3
#
# Prerequisites:
#   - AWS CLI configured (aws configure) or EC2 instance role
#   - mc (MinIO Client) installed: brew install minio/stable/mc
#   - S3 bucket already created
#
# Usage:
#   export AWS_REGION=ap-south-1
#   export S3_BUCKET=edutech-platform-prod
#   bash 07-export-minio-to-s3.sh                    # live mirror (recommended)
#   bash 07-export-minio-to-s3.sh --from-backup       # upload from local tar.gz
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
DUMPS_DIR="${SCRIPT_DIR}/../dumps/minio"

[[ -f "$ENV_FILE" ]] || { echo "ERROR: .env not found"; exit 1; }
source "$ENV_FILE"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

AWS_REGION="${AWS_REGION:-ap-south-1}"
S3_BUCKET="${S3_BUCKET:-}"
FROM_BACKUP=false
[[ "${1:-}" == "--from-backup" ]] && FROM_BACKUP=true

if [[ -z "$S3_BUCKET" ]]; then
  echo -e "${RED}ERROR: Set S3_BUCKET environment variable.${NC}"
  echo "  export S3_BUCKET=edutech-platform-prod"
  exit 1
fi

echo "╔═══════════════════════════════════════════════╗"
echo "║     MinIO → AWS S3 Migration                  ║"
echo "╠═══════════════════════════════════════════════╣"
echo "║  Source:   ${MINIO_ENDPOINT}/${MINIO_BUCKET_NAME}"
echo "║  Target:   s3://${S3_BUCKET}"
echo "║  Region:   ${AWS_REGION}"
echo "╚═══════════════════════════════════════════════╝"

if [[ "$FROM_BACKUP" == "true" ]]; then
  # ── Mode 2: Upload from local tar.gz backup ───────────────────────────────
  LATEST_ARCHIVE=$(ls -t "${DUMPS_DIR}"/minio_backup_*.tar.gz 2>/dev/null | head -1)
  if [[ -z "$LATEST_ARCHIVE" ]]; then
    echo -e "${RED}ERROR: No minio_backup_*.tar.gz found. Run 03-backup-minio.sh first.${NC}"
    exit 1
  fi

  echo "  Source archive: ${LATEST_ARCHIVE}"
  TMPDIR=$(mktemp -d)
  trap 'rm -rf "$TMPDIR"' EXIT

  echo "  Extracting archive..."
  tar -xzf "$LATEST_ARCHIVE" -C "$TMPDIR"

  echo "  Uploading to S3..."
  aws s3 sync "$TMPDIR" "s3://${S3_BUCKET}" \
    --region "$AWS_REGION" \
    --storage-class STANDARD \
    --sse aws:kms \
    --no-progress

  echo -e "  ${GREEN}✓${NC} Upload from backup complete"

else
  # ── Mode 1: Live mirror from MinIO → S3 (preferred) ──────────────────────
  echo "  Configuring mc aliases..."

  # Local MinIO
  mc alias set local-minio "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" --api S3v4 2>/dev/null

  # AWS S3 — uses AWS CLI credentials
  mc alias set aws-s3 "https://s3.${AWS_REGION}.amazonaws.com" \
    "${AWS_ACCESS_KEY_ID:-}" "${AWS_SECRET_ACCESS_KEY:-}" 2>/dev/null || \
  mc alias set aws-s3 "https://s3.${AWS_REGION}.amazonaws.com" "" "" --api S3v4 2>/dev/null

  echo "  Starting live mirror: MinIO/${MINIO_BUCKET_NAME} → S3/${S3_BUCKET}"
  mc mirror \
    --preserve \
    --watch=false \
    "local-minio/${MINIO_BUCKET_NAME}" \
    "aws-s3/${S3_BUCKET}"

  OBJECT_COUNT=$(mc ls --recursive "aws-s3/${S3_BUCKET}" 2>/dev/null | wc -l | tr -d ' ')
  echo -e "  ${GREEN}✓${NC} Migration complete. S3 objects: ${OBJECT_COUNT}"
fi

echo
echo "Post-migration verification:"
echo "  aws s3 ls s3://${S3_BUCKET} --recursive | wc -l"
echo
echo "Update center-svc environment on EC2:"
echo "  MINIO_ENDPOINT=https://s3.${AWS_REGION}.amazonaws.com"
echo "  MINIO_BUCKET_NAME=${S3_BUCKET}"
echo "  (Use IAM instance role — no MINIO_ACCESS_KEY/SECRET_KEY needed on EC2)"
