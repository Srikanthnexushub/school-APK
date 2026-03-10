#!/usr/bin/env bash
# =============================================================================
# EduTech AI Platform — Health Check
# =============================================================================
# Usage:
#   bash scripts/health-check.sh          # full table
#   bash scripts/health-check.sh --quiet  # one-liner per service (used by start-all)
#   bash scripts/health-check.sh --watch  # refresh every 5s (Ctrl+C to exit)
# =============================================================================

QUIET=false
WATCH=false
for arg in "$@"; do
  case $arg in
    --quiet) QUIET=true ;;
    --watch) WATCH=true ;;
  esac
done

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

# Format: "display-name|port"
SERVICES=(
  "api-gateway|8180"
  "auth-svc|8182"
  "parent-svc|8082"
  "center-svc|8083"
  "assess-svc|8084"
  "psych-svc|8085"
  "ai-gateway-svc|8086"
  "career-oracle-svc|8087"
  "mentor-svc|8088"
  "student-gateway|8089"
  "student-profile-svc|8090"
  "exam-tracker-svc|8091"
  "performance-svc|8092"
  "ai-mentor-svc|8093"
  "notification-svc|8094"
)

DOCKER_CONTAINERS=(
  "edutech-postgres|5433"
  "edutech-redis|6379"
  "edutech-kafka|9092"
)

check_svc() {
  local name="$1"
  local port="$2"
  local result
  result=$(curl -s --max-time 3 "http://localhost:${port}/actuator/health" 2>/dev/null || echo "")
  local status
  status=$(echo "${result}" | grep -o '"status":"[A-Z]*"' | head -1 | cut -d'"' -f4)
  if [[ "${status}" == "UP" ]]; then
    echo "UP"
  elif [[ -n "${status}" ]]; then
    echo "${status}"
  else
    echo "DOWN"
  fi
}

check_docker() {
  local container="$1"
  local state
  state=$(docker inspect --format='{{.State.Health.Status}}' "${container}" 2>/dev/null || echo "absent")
  if [[ "${state}" == "absent" ]]; then
    state=$(docker inspect --format='{{.State.Status}}' "${container}" 2>/dev/null || echo "absent")
  fi
  echo "${state}"
}

status_color() {
  case "$1" in
    UP|healthy|running) echo -e "${GREEN}$1${NC}" ;;
    STARTING|starting)  echo -e "${YELLOW}$1${NC}" ;;
    *)                  echo -e "${RED}$1${NC}" ;;
  esac
}

print_table() {
  if ! $QUIET; then
    echo ""
    echo -e "${CYAN}${BOLD}Infrastructure Containers${NC}"
    printf "  %-30s %-10s\n" "Container" "State"
    printf "  %-30s %-10s\n" "─────────────────────────────" "─────────"
  fi

  for entry in "${DOCKER_CONTAINERS[@]}"; do
    name="${entry%%|*}"
    port="${entry##*|}"
    state=$(check_docker "${name}")
    if $QUIET; then
      echo -e "  $(status_color "${state}")  ${name} (docker)"
    else
      printf "  %-30s " "${name}"
      echo -e "$(status_color "${state}")"
    fi
  done

  if ! $QUIET; then
    echo ""
    echo -e "${CYAN}${BOLD}Java Services${NC}"
    printf "  %-30s %-8s %s\n" "Service" "Port" "Health"
    printf "  %-30s %-8s %s\n" "─────────────────────────────" "────────" "──────"
  fi

  local up=0 down=0 total=${#SERVICES[@]}
  for entry in "${SERVICES[@]}"; do
    name="${entry%%|*}"
    port="${entry##*|}"
    status=$(check_svc "${name}" "${port}")
    if [[ "${status}" == "UP" ]]; then ((up++)) || true; else ((down++)) || true; fi
    if $QUIET; then
      echo -e "  $(status_color "${status}")  ${name} :${port}"
    else
      printf "  %-30s %-8s " "${name}" ":${port}"
      echo -e "$(status_color "${status}")"
    fi
  done

  if ! $QUIET; then
    echo ""
    if [[ $down -eq 0 ]]; then
      echo -e "  ${GREEN}${BOLD}All ${total} services UP${NC}"
    else
      echo -e "  ${GREEN}${up} UP${NC}  ${RED}${down} DOWN${NC}  (${total} total)"
    fi
    echo ""
  fi
}

if $WATCH; then
  while true; do
    clear
    echo -e "${BOLD}EduTech Health Check${NC}  ($(date '+%H:%M:%S'))  Ctrl+C to exit"
    QUIET=false
    print_table
    sleep 5
  done
else
  print_table
fi
