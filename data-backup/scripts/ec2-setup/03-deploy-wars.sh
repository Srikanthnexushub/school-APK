#!/usr/bin/env bash
# =============================================================================
# EC2 Setup Step 3: Deploy WARs to Tomcat instances + place exec WARs
#
# Prerequisites:
#   - All WARs transferred to /opt/apps/ and /opt/apps/exec-jars/
#   - Tomcat instances created (step 2)
#   - JWT keys in /opt/keys/
#
# Usage:
#   bash 03-deploy-wars.sh
#   bash 03-deploy-wars.sh --dry-run    # show what would be deployed
# =============================================================================
set -euo pipefail

DRY_RUN=false
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=true

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

# Service → Tomcat instance name (11 services — ai-mentor-svc runs as java -jar JAR)
declare -A TOMCAT_WARS=(
  [auth-svc]="auth-svc"
  [parent-svc]="parent-svc"
  [center-svc]="center-svc"
  [assess-svc]="assess-svc"
  [psych-svc]="psych-svc"
  [student-profile-svc]="student-profile-svc"
  [exam-tracker-svc]="exam-tracker-svc"
  [performance-svc]="performance-svc"
  [career-oracle-svc]="career-oracle-svc"
  [mentor-svc]="mentor-svc"
  [notification-svc]="notification-svc"
)

# api-gateway + student-gateway: Spring Cloud Gateway → produce JAR (not WAR)
# ai-gateway-svc: Netty exec WAR → use the -exec.war suffix
# ai-mentor-svc: no WAR packaging → produces JAR, runs as java -jar
EXEC_WARS=(api-gateway student-gateway ai-gateway-svc ai-mentor-svc)

echo "=== WAR Deployment ==="
echo "    Source: /opt/apps/"
echo "    Exec:   /opt/apps/exec-jars/"
echo

PASS=0; FAIL=0; true  # initialize counters (avoid set -e trap on ((n++)) when n=0)

# Deploy Tomcat WARs as ROOT.war
echo "--- Tomcat WARs (12) ---"
for SVC in "${!TOMCAT_WARS[@]}"; do
  INSTANCE=${TOMCAT_WARS[$SVC]}
  WAR=$(ls /opt/apps/${SVC}-*.war 2>/dev/null | head -1)
  TARGET="/opt/apps/tomcat-${INSTANCE}/webapps/ROOT.war"

  if [[ -z "$WAR" ]]; then
    echo -e "  ${RED}✗${NC} ${SVC}: WAR not found in /opt/apps/"
    FAIL=$((FAIL + 1))
    continue
  fi

  if [[ "$DRY_RUN" == "true" ]]; then
    echo -e "  ${YELLOW}[DRY-RUN]${NC} ${SVC}: $(basename $WAR) → ${TARGET}"
    continue
  fi

  # Remove old deployment if exists (prevents Tomcat from using stale exploded dir)
  rm -rf "/opt/apps/tomcat-${INSTANCE}/webapps/ROOT" 2>/dev/null || true
  rm -f "$TARGET" 2>/dev/null || true

  cp "$WAR" "$TARGET"
  SIZE=$(du -sh "$TARGET" | cut -f1)
  echo -e "  ${GREEN}✓${NC} ${SVC}: $(basename $WAR) → ROOT.war (${SIZE})"
  PASS=$((PASS + 1))
done

echo
echo "--- Exec artifacts (java -jar) ---"
for SVC in "${EXEC_WARS[@]}"; do
  # ai-gateway-svc uses -exec.war; all others use .jar
  if [[ "$SVC" == "ai-gateway-svc" ]]; then
    ARTIFACT=$(ls /opt/apps/exec-jars/${SVC}-*-exec.war 2>/dev/null | head -1)
  else
    ARTIFACT=$(ls /opt/apps/exec-jars/${SVC}-*.jar 2>/dev/null | head -1)
  fi
  if [[ -z "$ARTIFACT" ]]; then
    echo -e "  ${RED}✗${NC} ${SVC}: not found in /opt/apps/exec-jars/"
    FAIL=$((FAIL + 1))
  else
    SIZE=$(du -sh "$ARTIFACT" | cut -f1)
    echo -e "  ${GREEN}✓${NC} ${SVC}: $(basename $ARTIFACT) (${SIZE}) — ready for java -jar"
    $DRY_RUN || PASS=$((PASS + 1))
  fi
done

echo
echo "--- JWT Keys ---"
for KEY in jwt-private.pem jwt-public.pem; do
  if [[ -f "/opt/keys/${KEY}" ]]; then
    PERMS=$(stat -c "%a" "/opt/keys/${KEY}" 2>/dev/null || stat -f "%OLp" "/opt/keys/${KEY}")
    echo -e "  ${GREEN}✓${NC} /opt/keys/${KEY} (permissions: ${PERMS})"
  else
    echo -e "  ${RED}✗${NC} /opt/keys/${KEY}: NOT FOUND"
    ((FAIL++))
  fi
done

echo
echo "=== Deploy result: ${PASS} succeeded, ${FAIL} failed ==="
[[ $FAIL -gt 0 ]] && exit 1 || exit 0
