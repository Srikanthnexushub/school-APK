#!/usr/bin/env bash
# =============================================================================
# EduTech AI Platform — Restart a Single Service
# =============================================================================
# Usage:
#   bash scripts/restart-svc.sh auth-svc
#   bash scripts/restart-svc.sh assess-svc --no-build
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOGS_DIR="${ROOT_DIR}/logs"
PIDS_DIR="${ROOT_DIR}/.pids"
ENV_FILE="${ROOT_DIR}/.env"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }

if [[ $# -lt 1 ]]; then
  error "Usage: bash scripts/restart-svc.sh <service-name> [--no-build]"
  echo  "  Available: auth-svc center-svc parent-svc assess-svc psych-svc"
  echo  "             ai-gateway-svc career-oracle-svc mentor-svc"
  echo  "             student-profile-svc exam-tracker-svc performance-svc"
  echo  "             ai-mentor-svc notification-svc api-gateway student-gateway"
  exit 1
fi

SVC_NAME="$1"
DO_BUILD=true
[[ "${2:-}" == "--no-build" ]] && DO_BUILD=false

# ── service → port map ────────────────────────────────────────────────────────
declare -A PORT_MAP=(
  [auth-svc]=8182
  [parent-svc]=8082
  [center-svc]=8083
  [assess-svc]=8084
  [psych-svc]=8085
  [ai-gateway-svc]=8086
  [career-oracle-svc]=8087
  [mentor-svc]=8088
  [student-gateway]=8089
  [student-profile-svc]=8090
  [exam-tracker-svc]=8091
  [performance-svc]=8092
  [ai-mentor-svc]=8093
  [notification-svc]=8094
  [api-gateway]=8180
)

if [[ -z "${PORT_MAP[${SVC_NAME}]:-}" ]]; then
  error "Unknown service: ${SVC_NAME}"
  exit 1
fi

PORT="${PORT_MAP[${SVC_NAME}]}"
MODULE_PATH="services/${SVC_NAME}"

# ── load env ──────────────────────────────────────────────────────────────────
set +e
export $(grep -v '^#' "${ENV_FILE}" | grep -v '^ *$' | xargs) 2>/dev/null
set -e

mkdir -p "${LOGS_DIR}" "${PIDS_DIR}"

# ── stop existing ─────────────────────────────────────────────────────────────
pid_file="${PIDS_DIR}/${SVC_NAME}.pid"
if [[ -f "${pid_file}" ]]; then
  pid=$(cat "${pid_file}")
  if kill -0 "${pid}" 2>/dev/null; then
    info "Stopping ${SVC_NAME} (PID ${pid})..."
    kill -TERM "${pid}" 2>/dev/null || true
    for i in $(seq 1 10); do
      if ! kill -0 "${pid}" 2>/dev/null; then break; fi
      sleep 1
    done
    if kill -0 "${pid}" 2>/dev/null; then
      warn "Force killing ${SVC_NAME}..."
      kill -KILL "${pid}" 2>/dev/null || true
    fi
  fi
  rm -f "${pid_file}"
fi

# ── build ─────────────────────────────────────────────────────────────────────
if $DO_BUILD; then
  info "Building ${SVC_NAME}..."
  cd "${ROOT_DIR}"
  mvn package -pl "${MODULE_PATH}" --also-make -DskipTests --no-transfer-progress -q
fi

# ── start ─────────────────────────────────────────────────────────────────────
log_file="${LOGS_DIR}/${SVC_NAME}.log"
info "Starting ${SVC_NAME} on port ${PORT}..."

JAR=$(find "${ROOT_DIR}/${MODULE_PATH}/target" -name "*.jar" -not -name "*sources*" 2>/dev/null | head -1 || true)

if [[ -n "${JAR}" ]]; then
  java -jar "${JAR}" > "${log_file}" 2>&1 &
else
  cd "${ROOT_DIR}"
  mvn -pl "${MODULE_PATH}" spring-boot:run --no-transfer-progress -q > "${log_file}" 2>&1 &
fi

echo $! > "${pid_file}"
info "  PID $(cat "${pid_file}") → ${log_file}"

# ── wait for health ───────────────────────────────────────────────────────────
info "Waiting for ${SVC_NAME} to become healthy..."
for i in $(seq 1 40); do
  status=$(curl -s --max-time 2 "http://localhost:${PORT}/actuator/health" 2>/dev/null \
    | grep -o '"status":"[A-Z]*"' | head -1 | cut -d'"' -f4 || true)
  if [[ "${status}" == "UP" ]]; then
    echo -e "${GREEN}[INFO]${NC}  ${SVC_NAME} is UP on :${PORT}"
    exit 0
  fi
  sleep 3
done

warn "${SVC_NAME} did not report UP within 120s. Check: tail -f ${log_file}"
