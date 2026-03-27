#!/usr/bin/env bash
# =============================================================================
# 04-backup-keys.sh — Backup JWT RSA keys and environment config
#
# ⚠ SECURITY: The output of this script contains PRIVATE KEYS.
#   - Store the backup in an encrypted vault (AWS Secrets Manager / 1Password)
#   - NEVER commit the output to git
#   - NEVER upload to S3 without server-side encryption
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
DUMPS_DIR="${SCRIPT_DIR}/../dumps/keys"

[[ -f "$ENV_FILE" ]] || { echo "ERROR: .env not found at $ENV_FILE"; exit 1; }
source "$ENV_FILE"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${DUMPS_DIR}/${TIMESTAMP}"
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

echo "=== JWT Keys + Config Backup [${TIMESTAMP}] ==="
echo -e "  ${YELLOW}⚠ Output contains private keys — handle securely${NC}"

# 1. Copy JWT keys
JWT_PRIVATE="${PROJECT_ROOT}/keys/jwt-private.pem"
JWT_PUBLIC="${PROJECT_ROOT}/keys/jwt-public.pem"

if [[ -f "$JWT_PRIVATE" ]]; then
  cp "$JWT_PRIVATE" "${BACKUP_DIR}/jwt-private.pem"
  chmod 600 "${BACKUP_DIR}/jwt-private.pem"
  echo -e "  ${GREEN}✓${NC} jwt-private.pem backed up"
else
  echo -e "  ${RED}✗${NC} jwt-private.pem NOT FOUND at ${JWT_PRIVATE}"
fi

if [[ -f "$JWT_PUBLIC" ]]; then
  cp "$JWT_PUBLIC" "${BACKUP_DIR}/jwt-public.pem"
  chmod 644 "${BACKUP_DIR}/jwt-public.pem"
  echo -e "  ${GREEN}✓${NC} jwt-public.pem backed up"
else
  echo -e "  ${RED}✗${NC} jwt-public.pem NOT FOUND at ${JWT_PUBLIC}"
fi

# 2. Print key fingerprint for verification (safe to log)
if [[ -f "${BACKUP_DIR}/jwt-private.pem" ]]; then
  FINGERPRINT=$(openssl rsa -in "${BACKUP_DIR}/jwt-private.pem" -pubout 2>/dev/null | openssl pkey -pubin -fingerprint -sha256 -noout 2>/dev/null || echo "fingerprint-unavailable")
  echo "  Private key SHA256 fingerprint: ${FINGERPRINT}"
fi

# 3. Create encrypted .env backup (requires gpg)
if command -v gpg &>/dev/null; then
  echo -n "  Creating GPG-encrypted .env backup... "
  gpg --symmetric --cipher-algo AES256 --output "${BACKUP_DIR}/env.gpg" "$ENV_FILE" 2>/dev/null && \
    echo -e "${GREEN}✓${NC} env.gpg created (AES256)" || \
    echo -e "${YELLOW}skipped (gpg passphrase required)${NC}"
else
  # Fallback: copy with restricted permissions (no encryption)
  cp "$ENV_FILE" "${BACKUP_DIR}/.env.backup"
  chmod 600 "${BACKUP_DIR}/.env.backup"
  echo -e "  ${YELLOW}⚠${NC} .env copied without encryption (gpg not installed)"
fi

# 4. Document JWT config for AWS migration
cat > "${BACKUP_DIR}/jwt-config.txt" << EOF
# JWT Configuration — Required on AWS
# These must match EXACTLY in .env.prod for tokens to validate across services
JWT_ISSUER=${JWT_ISSUER}
JWT_ACCESS_TOKEN_EXPIRY_SECONDS=${JWT_ACCESS_TOKEN_EXPIRY_SECONDS}
JWT_REFRESH_TOKEN_EXPIRY_SECONDS=${JWT_REFRESH_TOKEN_EXPIRY_SECONDS}
JWT_JWKS_CACHE_TTL_SECONDS=${JWT_JWKS_CACHE_TTL_SECONDS}
# On AWS: JWT_PRIVATE_KEY_PATH=/opt/keys/jwt-private.pem
# On AWS: JWT_PUBLIC_KEY_PATH=/opt/keys/jwt-public.pem
# On AWS: JWT_JWKS_URI=http://127.0.0.1:8182/api/v1/auth/jwks
EOF
chmod 600 "${BACKUP_DIR}/jwt-config.txt"

# 5. Manifest
cat > "${BACKUP_DIR}/manifest.txt" << EOF
Keys backup: ${TIMESTAMP}
Files:
  jwt-private.pem   (RSA private key — KEEP SECRET)
  jwt-public.pem    (RSA public key — safe to share)
  jwt-config.txt    (JWT env var reference)
  env.gpg           (encrypted .env — gpg AES256)
EOF

echo "=== Keys backup complete: ${BACKUP_DIR} ==="
echo -e "  ${YELLOW}Store jwt-private.pem and env.gpg in AWS Secrets Manager or 1Password.${NC}"
