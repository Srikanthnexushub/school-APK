#!/usr/bin/env bash
# =============================================================================
# EC2 Setup Step 2: Install Tomcat 11 + create 12 service instances
# Run as ec2-user on Amazon Linux 2023 AFTER step 1
# =============================================================================
set -euo pipefail

TOMCAT_VER=11.0.4
TOMCAT_URL="https://dlcdn.apache.org/tomcat/tomcat-11/v${TOMCAT_VER}/bin/apache-tomcat-${TOMCAT_VER}.tar.gz"

echo "=== Installing Tomcat ${TOMCAT_VER} ==="

cd /tmp
[[ -f "apache-tomcat-${TOMCAT_VER}.tar.gz" ]] || wget -q "$TOMCAT_URL"
sudo mkdir -p /opt/apps/tomcat-base
sudo tar -xzf "apache-tomcat-${TOMCAT_VER}.tar.gz" -C /opt/apps/tomcat-base --strip-components=1
sudo chown -R ec2-user:ec2-user /opt/apps/tomcat-base
echo "Tomcat ${TOMCAT_VER} extracted ✓"

# Service → HTTP port → Shutdown port
# NOTE: ai-mentor-svc is NOT here — it has no WAR packaging, runs as java -jar (JAR)
# 12 Tomcat WAR services (added nexus-chat-svc):
declare -A SERVICE_HTTP=(
  [auth-svc]=8182
  [parent-svc]=8082
  [center-svc]=8083
  [assess-svc]=8084
  [psych-svc]=8085
  [student-profile-svc]=8090
  [exam-tracker-svc]=8091
  [performance-svc]=8092
  [career-oracle-svc]=8087
  [mentor-svc]=8088
  [notification-svc]=8094
  [nexus-chat-svc]=8097
)

declare -A SERVICE_SHUTDOWN=(
  [auth-svc]=9182
  [parent-svc]=9082
  [center-svc]=9083
  [assess-svc]=9084
  [psych-svc]=9085
  [student-profile-svc]=9090
  [exam-tracker-svc]=9091
  [performance-svc]=9192   # 9092 = Kafka — use 9192
  [career-oracle-svc]=9087
  [mentor-svc]=9088
  [notification-svc]=9094
  [nexus-chat-svc]=9097
)

echo "=== Creating 12 Tomcat instances ==="

for SVC in "${!SERVICE_HTTP[@]}"; do
  HTTP_PORT=${SERVICE_HTTP[$SVC]}
  SHUTDOWN_PORT=${SERVICE_SHUTDOWN[$SVC]}
  INSTANCE="/opt/apps/tomcat-${SVC}"

  if [[ -d "$INSTANCE" ]]; then
    echo "  ⚠ ${SVC}: already exists, skipping"
    continue
  fi

  cp -r /opt/apps/tomcat-base "$INSTANCE"

  # Set HTTP port
  sed -i "s/port=\"8080\"/port=\"${HTTP_PORT}\"/" "${INSTANCE}/conf/server.xml"
  # Set unique shutdown port
  sed -i "s/port=\"8005\"/port=\"${SHUTDOWN_PORT}\"/" "${INSTANCE}/conf/server.xml"
  # Remove AJP connector (security best practice, not needed)
  sed -i '/<Connector protocol="AJP/,/\/>/d' "${INSTANCE}/conf/server.xml"
  # Remove default webapps (security)
  rm -rf "${INSTANCE}/webapps/ROOT" "${INSTANCE}/webapps/docs" \
         "${INSTANCE}/webapps/examples" "${INSTANCE}/webapps/host-manager" \
         "${INSTANCE}/webapps/manager"
  mkdir -p "${INSTANCE}/webapps"

  # Create setenv.sh — sources .env.prod
  cat > "${INSTANCE}/bin/setenv.sh" << 'SETENV'
#!/bin/bash
set -a
source /opt/apps/.env.prod
set +a
export CATALINA_OPTS="-server -Xms256m -Xmx768m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Djava.security.egd=file:/dev/urandom \
  -Dfile.encoding=UTF-8 \
  -Djava.net.preferIPv4Stack=true"
SETENV
  chmod +x "${INSTANCE}/bin/setenv.sh"

  echo "  ✓ tomcat-${SVC} (HTTP:${HTTP_PORT}, shutdown:${SHUTDOWN_PORT})"
done

echo
echo "=== Verifying all 12 instances ==="
for SVC in "${!SERVICE_HTTP[@]}"; do
  HTTP_PORT=${SERVICE_HTTP[$SVC]}
  ACTUAL_PORT=$(grep 'Connector port=' /opt/apps/tomcat-${SVC}/conf/server.xml | grep -v AJP | head -1 | grep -o 'port="[0-9]*"' | grep -o '[0-9]*')
  [[ "$ACTUAL_PORT" == "$HTTP_PORT" ]] && \
    echo "  ✓ tomcat-${SVC}: port ${ACTUAL_PORT}" || \
    echo "  ✗ tomcat-${SVC}: expected ${HTTP_PORT}, got ${ACTUAL_PORT}"
done

echo
echo "=== Tomcat setup COMPLETE ==="
