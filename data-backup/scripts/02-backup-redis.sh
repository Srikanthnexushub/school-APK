#!/usr/bin/env bash
# =============================================================================
# 02-backup-redis.sh — Save Redis RDB snapshot to dumps/redis/
#
# Note: Redis in this project holds sessions, rate-limit counters, JWKS cache,
#       and OTP codes. Sessions expire naturally. JWKS/OTP are transient.
#       This backup is for completeness; on AWS use ElastiCache automatic backups.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
DUMPS_DIR="${SCRIPT_DIR}/../dumps/redis"

[[ -f "$ENV_FILE" ]] || { echo "ERROR: .env not found at $ENV_FILE"; exit 1; }
source "$ENV_FILE"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${DUMPS_DIR}/${TIMESTAMP}"
mkdir -p "$BACKUP_DIR"

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'

echo "=== Redis Backup [${TIMESTAMP}] ==="
echo "   Host: ${REDIS_HOST}:${REDIS_PORT}"

# 1. Trigger BGSAVE (non-blocking snapshot)
echo -n "  Triggering BGSAVE... "
redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" BGSAVE 2>/dev/null
sleep 3  # wait for snapshot to complete

# 2. Wait for save to complete
RETRIES=10
for i in $(seq 1 $RETRIES); do
  LAST_SAVE=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" LASTSAVE 2>/dev/null)
  SAVING=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" INFO persistence 2>/dev/null | grep "rdb_bgsave_in_progress" | cut -d':' -f2 | tr -d '[:space:]')
  if [[ "$SAVING" == "0" ]]; then
    echo -e "${GREEN}✓${NC} Save complete (LASTSAVE epoch: ${LAST_SAVE})"
    break
  fi
  echo "  ... still saving (attempt $i/$RETRIES)"
  sleep 2
done

# 3. Copy the RDB file
RDB_PATHS=(
  "/usr/local/var/db/redis/dump.rdb"
  "/opt/homebrew/var/db/redis/dump.rdb"
  "/var/lib/redis/dump.rdb"
)

FOUND_RDB=""
for rdb in "${RDB_PATHS[@]}"; do
  if [[ -f "$rdb" ]]; then
    FOUND_RDB="$rdb"
    break
  fi
done

if [[ -n "$FOUND_RDB" ]]; then
  cp "$FOUND_RDB" "${BACKUP_DIR}/dump_${TIMESTAMP}.rdb"
  SIZE=$(du -sh "${BACKUP_DIR}/dump_${TIMESTAMP}.rdb" | cut -f1)
  echo -e "  ${GREEN}✓${NC} RDB copied from ${FOUND_RDB} (${SIZE})"
else
  echo -e "  ${RED}✗${NC} RDB file not found — manual copy needed"
  echo "  Tried: ${RDB_PATHS[*]}"
fi

# 4. Export key count info
KEY_COUNT=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" DBSIZE 2>/dev/null || echo "unknown")
echo "  Key count: ${KEY_COUNT}"
echo "Redis backup: ${BACKUP_DIR}/dump_${TIMESTAMP}.rdb (${KEY_COUNT} keys)" > "${BACKUP_DIR}/manifest.txt"

# Keep latest symlink
LATEST_LINK="${DUMPS_DIR}/latest"
rm -f "$LATEST_LINK"
ln -s "$BACKUP_DIR" "$LATEST_LINK"

echo "=== Redis backup complete: ${BACKUP_DIR} ==="
