#!/usr/bin/env bash
# =============================================================================
# 03-backup-minio.sh — Mirror MinIO bucket contents to local dumps/minio/
#
# Uses MinIO Client (mc). Install: brew install minio/stable/mc
#
# What this backs up:
#   - All objects in MINIO_BUCKET_NAME (edutech-library)
#   - Preserves directory structure
#   - Creates a tar.gz archive for transfer to AWS S3
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
DUMPS_DIR="${SCRIPT_DIR}/../dumps/minio"

[[ -f "$ENV_FILE" ]] || { echo "ERROR: .env not found at $ENV_FILE"; exit 1; }
source "$ENV_FILE"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${DUMPS_DIR}/${TIMESTAMP}"
mkdir -p "${BACKUP_DIR}/objects"

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'

echo "=== MinIO Backup [${TIMESTAMP}] ==="
echo "   Endpoint: ${MINIO_ENDPOINT}"
echo "   Bucket:   ${MINIO_BUCKET_NAME}"
echo "   Output:   ${BACKUP_DIR}"

# 1. Configure mc alias
mc alias set edutech-backup "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" --api S3v4 2>/dev/null
echo -e "  ${GREEN}✓${NC} mc alias configured"

# 2. Mirror bucket to local
echo "  Mirroring objects (this may take a while)..."
mc mirror \
  --preserve \
  "edutech-backup/${MINIO_BUCKET_NAME}" \
  "${BACKUP_DIR}/objects/${MINIO_BUCKET_NAME}"

OBJECT_COUNT=$(find "${BACKUP_DIR}/objects" -type f | wc -l | tr -d ' ')
echo -e "  ${GREEN}✓${NC} ${OBJECT_COUNT} objects downloaded"

# 3. Create tar.gz archive for easy S3 upload
ARCHIVE="${DUMPS_DIR}/minio_backup_${TIMESTAMP}.tar.gz"
echo "  Creating archive: ${ARCHIVE}"
tar -czf "$ARCHIVE" -C "${BACKUP_DIR}/objects" .

SIZE=$(du -sh "$ARCHIVE" | cut -f1)
CHECKSUM=$(md5 -q "$ARCHIVE" 2>/dev/null || md5sum "$ARCHIVE" | cut -d' ' -f1)
echo -e "  ${GREEN}✓${NC} Archive created (${SIZE})"

# 4. Write manifest
cat > "${BACKUP_DIR}/manifest.txt" << EOF
# MinIO Backup Manifest
# Timestamp: ${TIMESTAMP}
# Endpoint: ${MINIO_ENDPOINT}
# Bucket: ${MINIO_BUCKET_NAME}
# Object count: ${OBJECT_COUNT}
# Archive: minio_backup_${TIMESTAMP}.tar.gz
# Size: ${SIZE}
# MD5: ${CHECKSUM}
EOF

# 5. Remove uncompressed mirror (save disk space)
rm -rf "${BACKUP_DIR}/objects"

# Keep latest symlink
LATEST_LINK="${DUMPS_DIR}/latest"
rm -f "$LATEST_LINK"
ln -s "$BACKUP_DIR" "$LATEST_LINK"

echo "=== MinIO backup complete: ${ARCHIVE} ==="
