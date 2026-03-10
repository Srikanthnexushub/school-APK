#!/usr/bin/env bash
# =============================================================================
# EduTech AI Platform — Start All Services
# =============================================================================
# Usage:
#   bash scripts/start-all.sh            # start infra + all 15 services + frontend
#   bash scripts/start-all.sh --no-build # skip Maven build (jars already built)
#   bash scripts/start-all.sh --no-frontend
#   bash scripts/start-all.sh --infra-only
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOGS_DIR="${ROOT_DIR}/logs"
PIDS_DIR="${ROOT_DIR}/.pids"
ENV_FILE="${ROOT_DIR}/.env"
COMPOSE_FILE="${ROOT_DIR}/infrastructure/docker/docker-compose.yml"

# ── flags ─────────────────────────────────────────────────────────────────────
DO_BUILD=true
DO_FRONTEND=true
INFRA_ONLY=false

for arg in "$@"; do
  case $arg in
    --no-build)     DO_BUILD=false ;;
    --no-frontend)  DO_FRONTEND=false ;;
    --infra-only)   INFRA_ONLY=true; DO_BUILD=false; DO_FRONTEND=false ;;
  esac
done

# ── colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
section() { echo -e "\n${CYAN}${BOLD}═══ $* ═══${NC}"; }

# ── prereq check ──────────────────────────────────────────────────────────────
section "Prerequisites"

if ! command -v docker &>/dev/null; then
  error "Docker not found. Install Docker Desktop and retry."
  exit 1
fi

if ! docker info &>/dev/null; then
  error "Docker daemon is not running. Start Docker Desktop and retry."
  exit 1
fi

if ! command -v java &>/dev/null; then
  error "Java not found. Install JDK 17+ and retry."
  exit 1
fi

if ! command -v mvn &>/dev/null; then
  error "Maven not found. Install Maven 3.9+ and retry."
  exit 1
fi

info "Docker: $(docker --version | head -1)"
info "Java:   $(java -version 2>&1 | head -1)"
info "Maven:  $(mvn -version 2>&1 | head -1)"

# ── env ───────────────────────────────────────────────────────────────────────
section "Environment"

if [[ ! -f "${ENV_FILE}" ]]; then
  error ".env not found at ${ENV_FILE}"
  error "Run: cp .env.example .env  then fill in values."
  exit 1
fi

set +e
# shellcheck disable=SC2046
export $(grep -v '^#' "${ENV_FILE}" | grep -v '^ *$' | xargs) 2>/dev/null
set -e
info "Environment loaded from ${ENV_FILE}"

mkdir -p "${LOGS_DIR}" "${PIDS_DIR}"

# ── infrastructure ─────────────────────────────────────────────────────────────
section "Infrastructure (Docker)"

info "Starting Postgres / Redis / Kafka containers..."
docker compose \
  --file "${COMPOSE_FILE}" \
  --env-file "${ENV_FILE}" \
  up -d 2>&1 | tail -5

# wait until postgres is accepting connections
info "Waiting for Postgres on port ${POSTGRES_PORT:-5433}..."
for i in $(seq 1 30); do
  if docker exec edutech-postgres pg_isready -q -p 5432 2>/dev/null; then
    info "Postgres is ready."
    break
  fi
  if [[ $i -eq 30 ]]; then
    error "Postgres did not become ready after 30 attempts."
    exit 1
  fi
  sleep 2
done

# wait for Redis
info "Waiting for Redis on port ${REDIS_PORT:-6379}..."
for i in $(seq 1 15); do
  if docker exec edutech-redis redis-cli -a "${REDIS_PASSWORD:-redis_dev_pass_2026}" ping 2>/dev/null | grep -q PONG; then
    info "Redis is ready."
    break
  fi
  if [[ $i -eq 15 ]]; then
    warn "Redis ping timed out — continuing anyway."
  fi
  sleep 2
done

if $INFRA_ONLY; then
  info "--infra-only flag set. Infrastructure is up. Exiting."
  exit 0
fi

# ── build ─────────────────────────────────────────────────────────────────────
if $DO_BUILD; then
  section "Maven Build (skip tests)"
  cd "${ROOT_DIR}"
  mvn clean package -DskipTests --no-transfer-progress -q
  info "Build complete."
fi

# ── service launcher helpers ──────────────────────────────────────────────────

# start_svc <module-path> <display-name> <health-port> [health-path]
start_svc() {
  local module_path="$1"
  local name="$2"
  local port="$3"
  local health_path="${4:-/actuator/health}"
  local log_file="${LOGS_DIR}/${name}.log"
  local pid_file="${PIDS_DIR}/${name}.pid"

  if [[ -f "${pid_file}" ]]; then
    local old_pid
    old_pid=$(cat "${pid_file}")
    if kill -0 "${old_pid}" 2>/dev/null; then
      warn "${name} already running (PID ${old_pid}). Skipping."
      return
    fi
    rm -f "${pid_file}"
  fi

  info "Starting ${name} on port ${port}..."

  JAR=$(find "${ROOT_DIR}/${module_path}/target" -name "*.jar" -not -name "*sources*" 2>/dev/null | head -1)

  if [[ -n "${JAR}" ]]; then
    # run packaged JAR — faster startup
    java -jar "${JAR}" \
      > "${log_file}" 2>&1 &
  else
    # fallback: Maven spring-boot:run
    mvn -pl "${module_path}" spring-boot:run \
      --no-transfer-progress \
      -q \
      > "${log_file}" 2>&1 &
  fi

  echo $! > "${pid_file}"
  info "  PID $(cat "${pid_file}") → ${log_file}"
}

# wait_healthy <display-name> <port> [timeout_secs] [health-path]
# Accepts HTTP 200 (UP) or 503 (some component DOWN but app is running).
# Non-critical health components (mail, external APIs) may be DOWN in local dev.
wait_healthy() {
  local name="$1"
  local port="$2"
  local timeout="${3:-120}"
  local path="${4:-/actuator/health}"
  local elapsed=0

  printf "  Waiting for %-30s" "${name}..."
  while true; do
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${port}${path}" 2>/dev/null || echo "000")
    if [[ "${http_code}" == "200" ]]; then
      echo -e " ${GREEN}UP${NC}"
      return 0
    elif [[ "${http_code}" == "503" ]]; then
      # App is running but a non-critical component is down (e.g. mail in local dev)
      echo -e " ${YELLOW}RUNNING (partial DOWN — check logs/${name}.log)${NC}"
      return 0
    fi
    if [[ $elapsed -ge $timeout ]]; then
      echo -e " ${YELLOW}TIMEOUT${NC} (check logs/${name}.log)"
      return 1
    fi
    sleep 3
    elapsed=$((elapsed + 3))
    printf "."
  done
}

# ── WAVE 1: auth-svc ───────────────────────────────────────────────────────────
section "Wave 1 — auth-svc (JWT issuer)"
start_svc "services/auth-svc" "auth-svc" 8182
wait_healthy "auth-svc" 8182 120

# ── WAVE 2: core domain services ─────────────────────────────────────────────
section "Wave 2 — Core Domain Services"
start_svc "services/center-svc"     "center-svc"     8083
start_svc "services/parent-svc"     "parent-svc"     8082
start_svc "services/assess-svc"     "assess-svc"     8084
start_svc "services/psych-svc"      "psych-svc"      8085

wait_healthy "center-svc"  8083 120
wait_healthy "parent-svc"  8082 120
wait_healthy "assess-svc"  8084 120
wait_healthy "psych-svc"   8085 120

# ── WAVE 3: student/learning services ─────────────────────────────────────────
section "Wave 3 — Student & Learning Services"
start_svc "services/student-profile-svc" "student-profile-svc" 8090
start_svc "services/exam-tracker-svc"    "exam-tracker-svc"    8091
start_svc "services/performance-svc"     "performance-svc"     8092
start_svc "services/ai-mentor-svc"       "ai-mentor-svc"       8093
start_svc "services/career-oracle-svc"   "career-oracle-svc"   8087
start_svc "services/mentor-svc"          "mentor-svc"          8088

wait_healthy "student-profile-svc" 8090 120
wait_healthy "exam-tracker-svc"    8091 120
wait_healthy "performance-svc"     8092 120
wait_healthy "ai-mentor-svc"       8093 120
wait_healthy "career-oracle-svc"   8087 120
wait_healthy "mentor-svc"          8088 120

# ── WAVE 4: AI gateway + notification ─────────────────────────────────────────
section "Wave 4 — AI Gateway & Notification"
start_svc "services/ai-gateway-svc"   "ai-gateway-svc"   8086
start_svc "services/notification-svc" "notification-svc" 8094

wait_healthy "ai-gateway-svc"   8086 90
wait_healthy "notification-svc" 8094 90

# ── WAVE 5: gateways ──────────────────────────────────────────────────────────
section "Wave 5 — API Gateways"
start_svc "services/api-gateway"     "api-gateway"     8180
start_svc "services/student-gateway" "student-gateway" 8089

wait_healthy "api-gateway"     8180 90
wait_healthy "student-gateway" 8089 90

# ── WAVE 6: frontend ──────────────────────────────────────────────────────────
if $DO_FRONTEND; then
  section "Wave 6 — Frontend (Vite)"
  FRONTEND_DIR="${ROOT_DIR}/frontend/web"
  if [[ -d "${FRONTEND_DIR}" ]]; then
    if [[ ! -d "${FRONTEND_DIR}/node_modules" ]]; then
      info "Installing npm dependencies..."
      npm install --prefix "${FRONTEND_DIR}" --silent
    fi
    info "Starting Vite dev server..."
    npm run dev --prefix "${FRONTEND_DIR}" \
      > "${LOGS_DIR}/frontend.log" 2>&1 &
    echo $! > "${PIDS_DIR}/frontend.pid"
    info "  PID $(cat "${PIDS_DIR}/frontend.pid") → ${LOGS_DIR}/frontend.log"
    sleep 3
    VITE_PORT=$(grep -m1 'Local:' "${LOGS_DIR}/frontend.log" 2>/dev/null | grep -oE '[0-9]{4,5}$' || echo "5173")
    info "  Frontend → http://localhost:${VITE_PORT}"
  else
    warn "frontend/web not found — skipping."
  fi
fi

# ── summary ───────────────────────────────────────────────────────────────────
section "Stack Summary"
bash "${SCRIPT_DIR}/health-check.sh" --quiet 2>/dev/null || true

echo ""
echo -e "${BOLD}Useful URLs:${NC}"
echo "  Frontend         → http://localhost:5173"
echo "  API Gateway      → http://localhost:8180/actuator/health"
echo "  Student Gateway  → http://localhost:8089/actuator/health"
echo "  Kafka UI         → http://localhost:${KAFKA_UI_PORT:-9080}"
echo "  pgAdmin          → http://localhost:${PGADMIN_PORT:-5050}"
echo ""
echo -e "${BOLD}Logs:${NC}  tail -f ${LOGS_DIR}/<service>.log"
echo -e "${BOLD}Stop:${NC}  bash scripts/stop-all.sh"
echo ""
info "All services started. Happy coding!"
