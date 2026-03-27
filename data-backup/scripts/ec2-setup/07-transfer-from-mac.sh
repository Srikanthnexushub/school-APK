#!/usr/bin/env bash
# =============================================================================
# Mac-side script: Transfer all artifacts to EC2
#
# Run from your Mac AFTER:
#   1. Maven build completed (mvn clean package -DskipITs -T 4 -Drevision=1.0.0-PROD)
#   2. Frontend built (cd frontend/web && npm run build)
#   3. EC2 instance is running with SSH accessible
#
# Usage:
#   export EC2_IP=<your-ec2-public-ip>
#   export KEY_FILE=~/your-key.pem
#   bash 07-transfer-from-mac.sh
# =============================================================================
set -euo pipefail

EC2_IP="${EC2_IP:-}"
KEY_FILE="${KEY_FILE:-}"
EC2_USER="ec2-user"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

[[ -z "$EC2_IP" ]] && { echo "ERROR: Set EC2_IP env var"; exit 1; }
[[ -z "$KEY_FILE" ]] && { echo "ERROR: Set KEY_FILE env var"; exit 1; }
[[ -f "$KEY_FILE" ]] || { echo "ERROR: Key file not found: $KEY_FILE"; exit 1; }

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
SSH="ssh -i $KEY_FILE -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_IP}"
SCP="scp -i $KEY_FILE -o StrictHostKeyChecking=no"

echo "=== Transfer to EC2: ${EC2_IP} ==="
echo "   Project: ${PROJECT_ROOT}"

# ─── 1. Verify SSH ────────────────────────────────────────────────────────────
echo -n "  Testing SSH... "
$SSH "echo ok" | grep -q ok && echo -e "${GREEN}✓${NC}" || { echo -e "${RED}FAILED${NC}"; exit 1; }

# ─── 2. Create directories on EC2 ─────────────────────────────────────────────
$SSH "mkdir -p /opt/apps/exec-jars /opt/keys /opt/logs /opt/python-ai"
echo -e "  ${GREEN}✓${NC} EC2 directories ready"

# ─── 3. JWT keys (FIRST — services need these) ───────────────────────────────
echo -n "  Transferring JWT keys... "
$SCP "${PROJECT_ROOT}/keys/jwt-private.pem" "${EC2_USER}@${EC2_IP}:/opt/keys/"
$SCP "${PROJECT_ROOT}/keys/jwt-public.pem"  "${EC2_USER}@${EC2_IP}:/opt/keys/"
$SSH "chmod 600 /opt/keys/jwt-private.pem; chmod 644 /opt/keys/jwt-public.pem"
echo -e "${GREEN}✓${NC}"

# ─── 4. Tomcat WARs (11 services) ────────────────────────────────────────────
# NOTE: ai-mentor-svc has no WAR packaging — it goes to exec-jars/ below
echo "  Transferring Tomcat WARs (11)..."
TOMCAT_WARS=(
  "services/auth-svc/target/auth-svc-1.0.0-PROD.war"
  "services/parent-svc/target/parent-svc-1.0.0-PROD.war"
  "services/center-svc/target/center-svc-1.0.0-PROD.war"
  "services/assess-svc/target/assess-svc-1.0.0-PROD.war"
  "services/psych-svc/target/psych-svc-1.0.0-PROD.war"
  "services/student-profile-svc/target/student-profile-svc-1.0.0-PROD.war"
  "services/exam-tracker-svc/target/exam-tracker-svc-1.0.0-PROD.war"
  "services/performance-svc/target/performance-svc-1.0.0-PROD.war"
  "services/career-oracle-svc/target/career-oracle-svc-1.0.0-PROD.war"
  "services/mentor-svc/target/mentor-svc-1.0.0-PROD.war"
  "services/notification-svc/target/notification-svc-1.0.0-PROD.war"
)
MISSING=0
for WAR_PATH in "${TOMCAT_WARS[@]}"; do
  FULL_PATH="${PROJECT_ROOT}/${WAR_PATH}"
  if [[ -f "$FULL_PATH" ]]; then
    SVC=$(basename "$FULL_PATH" | sed 's/-1.0.0-PROD.war//')
    echo -n "    ${SVC}... "
    $SCP "$FULL_PATH" "${EC2_USER}@${EC2_IP}:/opt/apps/"
    echo -e "${GREEN}✓${NC}"
  else
    echo -e "    ${RED}✗${NC} NOT FOUND: ${FULL_PATH}"
    ((MISSING++))
  fi
done

# ─── 5. Exec artifacts (4 services — java -jar) ───────────────────────────────
# api-gateway + student-gateway → Spring Cloud Gateway → produce JAR (not WAR)
# ai-gateway-svc                → exec WAR (Netty embedded, not Tomcat)
# ai-mentor-svc                 → no WAR packaging → produces JAR only
echo "  Transferring exec artifacts (4)..."
EXEC_ARTIFACTS=(
  "services/api-gateway/target/api-gateway-1.0.0-PROD.jar"
  "services/student-gateway/target/student-gateway-1.0.0-PROD.jar"
  "services/ai-gateway-svc/target/ai-gateway-svc-1.0.0-PROD-exec.war"
  "services/ai-mentor-svc/target/ai-mentor-svc-1.0.0-PROD.jar"
)
for ARTIFACT_PATH in "${EXEC_ARTIFACTS[@]}"; do
  FULL_PATH="${PROJECT_ROOT}/${ARTIFACT_PATH}"
  if [[ -f "$FULL_PATH" ]]; then
    SVC=$(basename "$FULL_PATH" | sed 's/-1.0.0-PROD-exec\.war//' | sed 's/-1.0.0-PROD\.jar//')
    echo -n "    ${SVC}... "
    $SCP "$FULL_PATH" "${EC2_USER}@${EC2_IP}:/opt/apps/exec-jars/"
    echo -e "${GREEN}✓${NC}"
  else
    echo -e "    ${RED}✗${NC} NOT FOUND: ${FULL_PATH}"
    ((MISSING++))
  fi
done

# ─── 6. Python AI sidecar ────────────────────────────────────────────────────
echo -n "  Transferring python-ai-svc... "
$SCP -r "${PROJECT_ROOT}/python-ai-svc" "${EC2_USER}@${EC2_IP}:/opt/python-ai/"
echo -e "${GREEN}✓${NC}"

# ─── 7. Frontend dist ────────────────────────────────────────────────────────
DIST="${PROJECT_ROOT}/frontend/web/dist"
if [[ -d "$DIST" ]]; then
  echo -n "  Transferring frontend dist... "
  $SCP -r "${DIST}/." "${EC2_USER}@${EC2_IP}:/usr/share/nginx/html/"
  echo -e "${GREEN}✓${NC}"
else
  echo -e "  ${YELLOW}⚠${NC} Frontend dist not found — build first:"
  echo "       cd frontend/web && npm ci && npm run build"
fi

# ─── 8. EC2 setup scripts ────────────────────────────────────────────────────
echo -n "  Transferring EC2 setup scripts... "
$SCP -r "${PROJECT_ROOT}/data-backup/scripts/ec2-setup" "${EC2_USER}@${EC2_IP}:/opt/apps/"
$SSH "chmod +x /opt/apps/ec2-setup/*.sh"
echo -e "${GREEN}✓${NC}"

echo
echo "=== Transfer summary ==="
[[ $MISSING -gt 0 ]] && echo -e "${RED}${MISSING} artifacts missing — rebuild with mvn clean package -DskipTests -T 4 -Drevision=1.0.0-PROD${NC}" || \
  echo -e "${GREEN}All artifacts transferred successfully${NC}"

echo
echo "Next steps on EC2 (in order):"
echo "  1. Create /opt/apps/.env.prod  (fill in RDS/Redis/SES/AI credentials)"
echo "  2. bash /opt/apps/ec2-setup/02-tomcat-instances.sh"
echo "  3. bash /opt/apps/ec2-setup/03-deploy-wars.sh"
echo "  4. bash /opt/apps/ec2-setup/05-nginx-frontend.sh"
echo "  5. bash /opt/apps/ec2-setup/06-systemd-services.sh"
echo "  6. bash /opt/apps/ec2-setup/04-start-services.sh"
