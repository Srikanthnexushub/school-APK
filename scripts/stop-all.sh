#!/usr/bin/env bash
# =============================================================================
# EduTech AI Platform — Stop All Services
# =============================================================================
# Usage:
#   bash scripts/stop-all.sh            # stop services + keep infra running
#   bash scripts/stop-all.sh --all      # stop services + stop Docker infra
#   bash scripts/stop-all.sh --reset    # stop everything + wipe Docker volumes
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PIDS_DIR="${ROOT_DIR}/.pids"
ENV_FILE="${ROOT_DIR}/.env"
COMPOSE_FILE="${ROOT_DIR}/infrastructure/docker/docker-compose.yml"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
section() { echo -e "\n${CYAN}${BOLD}═══ $* ═══${NC}"; }

STOP_INFRA=false
RESET=false
for arg in "$@"; do
  case $arg in
    --all)   STOP_INFRA=true ;;
    --reset) STOP_INFRA=true; RESET=true ;;
  esac
done

# ── stop Java services (reverse startup order) ────────────────────────────────
section "Stopping Services"

SERVICES=(
  frontend
  student-gateway
  api-gateway
  notification-svc
  ai-gateway-svc
  mentor-svc
  career-oracle-svc
  ai-mentor-svc
  performance-svc
  exam-tracker-svc
  student-profile-svc
  psych-svc
  assess-svc
  parent-svc
  center-svc
  auth-svc
)

if [[ ! -d "${PIDS_DIR}" ]]; then
  warn "No .pids directory found — no managed processes to stop."
else
  for svc in "${SERVICES[@]}"; do
    pid_file="${PIDS_DIR}/${svc}.pid"
    if [[ -f "${pid_file}" ]]; then
      pid=$(cat "${pid_file}")
      if kill -0 "${pid}" 2>/dev/null; then
        info "Stopping ${svc} (PID ${pid})..."
        kill -TERM "${pid}" 2>/dev/null || true
        # give it up to 10s to exit gracefully
        for i in $(seq 1 10); do
          if ! kill -0 "${pid}" 2>/dev/null; then break; fi
          sleep 1
        done
        if kill -0 "${pid}" 2>/dev/null; then
          warn "  ${svc} did not exit — sending SIGKILL"
          kill -KILL "${pid}" 2>/dev/null || true
        fi
      else
        warn "${svc} PID ${pid} is not running (stale pid file)."
      fi
      rm -f "${pid_file}"
    fi
  done
fi

info "All managed services stopped."

# ── stop infrastructure ───────────────────────────────────────────────────────
if $STOP_INFRA; then
  section "Stopping Docker Infrastructure"
  set +e
  export $(grep -v '^#' "${ENV_FILE}" | grep -v '^ *$' | xargs) 2>/dev/null
  set -e

  if $RESET; then
    warn "RESET mode — all Docker volumes will be DELETED (data lost)."
    read -r -p "  Confirm reset? [y/N] " confirm
    if [[ "${confirm,,}" == "y" ]]; then
      docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" down -v
      info "Volumes deleted."
    else
      info "Reset cancelled — running docker compose down (keep volumes)."
      docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" down
    fi
  else
    docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" down
    info "Infrastructure containers stopped (volumes preserved)."
  fi
fi

echo ""
info "Done. Run 'bash scripts/start-all.sh --no-build' to restart."
