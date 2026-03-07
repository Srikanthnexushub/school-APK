#!/usr/bin/env bash
# =============================================================================
# EduTech AI Platform — Local Development Setup Script
# =============================================================================
# Run once after cloning the repo to prepare the local dev environment.
# Prerequisites: Docker, Java 21, Maven 3.9+, Node 20+
# Usage: bash scripts/local-dev-setup.sh
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Colours for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ---------------------------------------------------------------------------
# 1. Check prerequisites
# ---------------------------------------------------------------------------
log_info "Checking prerequisites..."

check_command() {
    if ! command -v "$1" &>/dev/null; then
        log_error "'$1' is not installed. Please install it and re-run."
        exit 1
    fi
}

check_command docker
check_command java
check_command mvn
check_command node
check_command npm

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [[ "${JAVA_VERSION}" -lt 21 ]]; then
    log_error "Java 21 or higher is required. Found: Java ${JAVA_VERSION}"
    exit 1
fi

log_info "All prerequisites satisfied."

# ---------------------------------------------------------------------------
# 2. Copy .env.example to .env if not present
# ---------------------------------------------------------------------------
ENV_FILE="${ROOT_DIR}/.env"
ENV_EXAMPLE="${ROOT_DIR}/.env.example"

if [[ ! -f "${ENV_FILE}" ]]; then
    cp "${ENV_EXAMPLE}" "${ENV_FILE}"
    log_warn ".env created from .env.example — FILL IN ALL VALUES before continuing."
    log_warn "Edit ${ENV_FILE} then re-run this script."
    exit 0
else
    log_info ".env already exists, skipping copy."
fi

# ---------------------------------------------------------------------------
# 3. Generate RSA key pair for JWT RS256 signing (if not present)
# ---------------------------------------------------------------------------
KEYS_DIR="${ROOT_DIR}/keys"
mkdir -p "${KEYS_DIR}"

if [[ ! -f "${KEYS_DIR}/jwt-private.pem" ]]; then
    log_info "Generating RSA-2048 key pair for JWT..."
    openssl genrsa -out "${KEYS_DIR}/jwt-private.pem" 2048
    openssl rsa -in "${KEYS_DIR}/jwt-private.pem" \
        -pubout -out "${KEYS_DIR}/jwt-public.pem"
    log_info "Keys written to ${KEYS_DIR}/ — update JWT_PRIVATE_KEY_PATH and JWT_PUBLIC_KEY_PATH in .env"
else
    log_info "JWT keys already exist, skipping generation."
fi

# ---------------------------------------------------------------------------
# 4. Start infrastructure containers
# ---------------------------------------------------------------------------
COMPOSE_FILE="${ROOT_DIR}/infrastructure/docker/docker-compose.yml"
log_info "Starting infrastructure containers..."

docker compose \
    --file "${COMPOSE_FILE}" \
    --env-file "${ENV_FILE}" \
    up -d --wait

log_info "Containers started. Waiting for health checks..."

# ---------------------------------------------------------------------------
# 5. Build all Maven modules (skipping tests for first-time setup)
# ---------------------------------------------------------------------------
log_info "Building Maven modules (skipping tests)..."
cd "${ROOT_DIR}"
mvn clean install -DskipTests --no-transfer-progress

log_info "Build complete."

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
log_info "================================================================"
log_info " Local dev environment is ready."
log_info " PostgreSQL : localhost:\${POSTGRES_PORT}"
log_info " Redis      : localhost:\${REDIS_PORT}"
log_info " Kafka      : localhost:\${KAFKA_EXTERNAL_PORT}"
log_info " Kafka UI   : http://localhost:\${KAFKA_UI_PORT}"
log_info " pgAdmin    : http://localhost:\${PGADMIN_PORT}"
log_info "================================================================"
log_info " To start a service:  mvn spring-boot:run -pl services/auth-svc"
log_info "================================================================"
