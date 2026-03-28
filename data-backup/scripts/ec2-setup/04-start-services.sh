#!/usr/bin/env bash
# =============================================================================
# EC2 Setup Step 4: Start all 15 services in correct dependency order
#
# Run this AFTER:
#   - All WARs deployed (step 3)
#   - .env.prod populated at /opt/apps/.env.prod
#   - Kafka running (step 1)
#   - RDS accessible (all 12 DBs created)
#   - Redis accessible
#
# Startup order (hard dependency chain):
#   Kafka → auth-svc → core services → student services →
#   python-ai-svc → ai-gateway-svc → student-gateway → api-gateway
# =============================================================================
set -euo pipefail

ENV_FILE="/opt/apps/.env.prod"
[[ -f "$ENV_FILE" ]] || { echo "ERROR: $ENV_FILE not found"; exit 1; }

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

health_check() {
  local SVC="$1"; local PORT="$2"; local RETRIES="${3:-20}"
  for i in $(seq 1 $RETRIES); do
    if curl -sf "http://127.0.0.1:${PORT}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
      echo -e "  ${GREEN}✓${NC} ${SVC} UP (${i}s)"
      return 0
    fi
    sleep 1
  done
  echo -e "  ${RED}✗${NC} ${SVC} NOT UP after ${RETRIES}s — check: tail -100 /opt/apps/tomcat-${SVC}/logs/catalina.out"
  return 1
}

FAIL=0

# ─── 0. Kafka ──────────────────────────────────────────────────────────────────
echo "=== [0/8] Kafka ==="
sudo systemctl is-active kafka &>/dev/null || sudo systemctl start kafka
sleep 3
/opt/kafka/bin/kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --list &>/dev/null && \
  echo -e "  ${GREEN}✓${NC} Kafka listening on 9092" || \
  { echo -e "  ${RED}✗${NC} Kafka not ready"; FAIL=$((FAIL + 1)); }

# ─── 1. auth-svc ──────────────────────────────────────────────────────────────
echo "=== [1/8] auth-svc (Flyway: 6 migrations) ==="
source "$ENV_FILE"
/opt/apps/tomcat-auth-svc/bin/startup.sh > /dev/null
health_check "auth-svc" 8182 40 || FAIL=$((FAIL + 1))

# ─── 2. Core domain services ───────────────────────────────────────────────────
echo "=== [2/8] Core domain (center-svc has 28 migrations — allow 60s) ==="
for SVC in center-svc parent-svc assess-svc psych-svc; do
  source "$ENV_FILE"
  echo -n "  Starting ${SVC}... "
  /opt/apps/tomcat-${SVC}/bin/startup.sh > /dev/null
  echo "started"
  sleep 3
done
echo "  Waiting for Flyway migrations (up to 60s)..."
sleep 30
for SVC_PORT in "center-svc:8083" "parent-svc:8082" "assess-svc:8084" "psych-svc:8085"; do
  SVC="${SVC_PORT%%:*}"; PORT="${SVC_PORT##*:}"
  health_check "$SVC" "$PORT" 30 || FAIL=$((FAIL + 1))
done

# ─── 3. Student portal Tomcat services ────────────────────────────────────────
# NOTE: ai-mentor-svc is NOT Tomcat — it has no WAR packaging, started separately below
echo "=== [3/8] Student portal Tomcat services ==="
for SVC in student-profile-svc exam-tracker-svc performance-svc \
           career-oracle-svc mentor-svc notification-svc; do
  source "$ENV_FILE"
  echo -n "  Starting ${SVC}... "
  /opt/apps/tomcat-${SVC}/bin/startup.sh > /dev/null
  echo "started"
  sleep 2
done
sleep 25
for SVC_PORT in "student-profile-svc:8090" "exam-tracker-svc:8091" "performance-svc:8092" \
                "career-oracle-svc:8087" "mentor-svc:8088" "notification-svc:8094"; do
  SVC="${SVC_PORT%%:*}"; PORT="${SVC_PORT##*:}"
  health_check "$SVC" "$PORT" 30 || FAIL=$((FAIL + 1))
done

# ─── 3b. ai-mentor-svc (JAR — java -jar, no WAR packaging) ────────────────────
echo "=== [3b/8] ai-mentor-svc (JAR) ==="
source "$ENV_FILE"
ARTIFACT=$(ls /opt/apps/exec-jars/ai-mentor-svc-*.jar 2>/dev/null | head -1)
[[ -z "$ARTIFACT" ]] && { echo -e "  ${RED}✗${NC} ai-mentor-svc JAR not found"; FAIL=$((FAIL + 1)); } || {
  nohup java -server -Xms256m -Xmx512m \
    -Djava.security.egd=file:/dev/urandom \
    -jar "$ARTIFACT" \
    > /opt/logs/ai-mentor-svc.log 2>&1 &
  echo $! > /opt/apps/ai-mentor-svc.pid
  health_check "ai-mentor-svc" 8093 30 || FAIL=$((FAIL + 1))
}

# ─── 4. Python AI sidecar ──────────────────────────────────────────────────────
echo "=== [4/8] Python AI sidecar ==="
cd /opt/python-ai/python-ai-svc
[[ -d .venv ]] || python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt -q
source "$ENV_FILE"
nohup uvicorn main:app --host 127.0.0.1 --port 8095 --workers 2 \
  > /opt/logs/python-ai-svc.log 2>&1 &
echo $! > /opt/apps/python-ai.pid
sleep 5
curl -sf http://127.0.0.1:8095/health &>/dev/null && \
  echo -e "  ${GREEN}✓${NC} python-ai-svc UP (port 8095)" || \
  echo -e "  ${YELLOW}⚠${NC} python-ai-svc health check pending"

# ─── 5. ai-gateway-svc ─────────────────────────────────────────────────────────
echo "=== [5/8] ai-gateway-svc (exec WAR — Netty) ==="
source "$ENV_FILE"
ARTIFACT=$(ls /opt/apps/exec-jars/ai-gateway-svc-*-exec.war 2>/dev/null | head -1)
[[ -z "$ARTIFACT" ]] && { echo -e "  ${RED}✗${NC} ai-gateway-svc exec WAR not found"; FAIL=$((FAIL + 1)); } || {
  nohup java -server -Xms256m -Xmx512m \
    -Djava.security.egd=file:/dev/urandom \
    -jar "$ARTIFACT" \
    > /opt/logs/ai-gateway-svc.log 2>&1 &
  echo $! > /opt/apps/ai-gateway-svc.pid
  health_check "ai-gateway-svc" 8086 30 || FAIL=$((FAIL + 1))
}

# ─── 6. student-gateway ────────────────────────────────────────────────────────
echo "=== [6/8] student-gateway (JAR — Spring Cloud Gateway) ==="
source "$ENV_FILE"
ARTIFACT=$(ls /opt/apps/exec-jars/student-gateway-*.jar 2>/dev/null | head -1)
[[ -z "$ARTIFACT" ]] && { echo -e "  ${RED}✗${NC} student-gateway JAR not found"; FAIL=$((FAIL + 1)); } || {
  nohup java -server -Xms256m -Xmx512m \
    -Djava.security.egd=file:/dev/urandom \
    -jar "$ARTIFACT" \
    > /opt/logs/student-gateway.log 2>&1 &
  echo $! > /opt/apps/student-gateway.pid
  health_check "student-gateway" 8089 30 || FAIL=$((FAIL + 1))
}

# ─── 7. api-gateway — START LAST ───────────────────────────────────────────────
echo "=== [7/8] api-gateway (JAR — Spring Cloud Gateway — STARTING LAST) ==="
source "$ENV_FILE"
ARTIFACT=$(ls /opt/apps/exec-jars/api-gateway-*.jar 2>/dev/null | head -1)
[[ -z "$ARTIFACT" ]] && { echo -e "  ${RED}✗${NC} api-gateway JAR not found"; FAIL=$((FAIL + 1)); } || {
  nohup java -server -Xms512m -Xmx1g \
    -Djava.security.egd=file:/dev/urandom \
    -jar "$ARTIFACT" \
    > /opt/logs/api-gateway.log 2>&1 &
  echo $! > /opt/apps/api-gateway.pid
  health_check "api-gateway" 8180 30 || FAIL=$((FAIL + 1))
}

# ─── 8. Final health check — all 15 services ───────────────────────────────────
echo
echo "=== [8/8] Full system health check ==="
declare -A ALL_SERVICES=(
  [auth-svc]=8182 [parent-svc]=8082 [center-svc]=8083 [assess-svc]=8084
  [psych-svc]=8085 [ai-gateway-svc]=8086 [career-oracle-svc]=8087
  [mentor-svc]=8088 [student-gateway]=8089 [student-profile-svc]=8090
  [exam-tracker-svc]=8091 [performance-svc]=8092 [ai-mentor-svc]=8093
  [notification-svc]=8094 [api-gateway]=8180
)
ALL_UP=true
for SVC in "${!ALL_SERVICES[@]}"; do
  PORT=${ALL_SERVICES[$SVC]}
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:${PORT}/actuator/health")
  if [[ "$STATUS" == "200" ]]; then
    echo -e "  ${GREEN}✓${NC} ${SVC}:${PORT}"
  else
    echo -e "  ${RED}✗${NC} ${SVC}:${PORT} → HTTP ${STATUS}"
    ALL_UP=false
    FAIL=$((FAIL + 1))
  fi
done

echo
if [[ $FAIL -eq 0 ]] && $ALL_UP; then
  echo -e "${GREEN}ALL 15 SERVICES UP — deployment successful${NC}"
  echo "  Test: curl http://127.0.0.1:8180/actuator/health"
  echo "  JWKS: curl http://127.0.0.1:8182/api/v1/auth/jwks"
else
  echo -e "${RED}${FAIL} service(s) failed to start${NC}"
  echo "  Diagnose: tail -100 /opt/apps/tomcat-<service>/logs/catalina.out"
  echo "  Diagnose: tail -100 /opt/logs/<service>.log"
  exit 1
fi
