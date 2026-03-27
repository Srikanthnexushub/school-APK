#!/usr/bin/env bash
# =============================================================================
# EC2 Setup Step 6: Create systemd services for all 15 Spring Boot services
# Enables auto-start on reboot and clean process management
# =============================================================================
set -euo pipefail

echo "=== Creating systemd services for all 15 Spring Boot services ==="
echo "    11 Tomcat WARs + 4 exec JARs/WARs + 1 Python sidecar"

# ─── Helper: create Tomcat systemd service ─────────────────────────────────────
create_tomcat_service() {
  local SVC="$1"
  sudo tee "/etc/systemd/system/edutech-${SVC}.service" > /dev/null << UNIT
[Unit]
Description=EduTech ${SVC} (Tomcat WAR)
After=network.target kafka.service edutech-auth-svc.service
Requires=network.target

[Service]
Type=forking
User=ec2-user
Environment="JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto"
EnvironmentFile=/opt/apps/.env.prod
ExecStart=/opt/apps/tomcat-${SVC}/bin/startup.sh
ExecStop=/opt/apps/tomcat-${SVC}/bin/shutdown.sh
Restart=on-failure
RestartSec=15s
TimeoutStartSec=120s
TimeoutStopSec=30s

[Install]
WantedBy=multi-user.target
UNIT
  echo "  ✓ edutech-${SVC}.service"
}

# ─── Helper: create exec artifact (java -jar) systemd service ─────────────────
# $4 = "war" for ai-gateway-svc (uses -exec.war), "jar" for api/student-gateway
create_exec_service() {
  local SVC="$1"; local PORT="$2"; local HEAP_MAX="${3:-512m}"; local EXT="${4:-jar}"
  if [[ "$EXT" == "war" ]]; then
    ARTIFACT_GLOB="/opt/apps/exec-jars/${SVC}-*-exec.war"
  else
    ARTIFACT_GLOB="/opt/apps/exec-jars/${SVC}-*.jar"
  fi
  sudo tee "/etc/systemd/system/edutech-${SVC}.service" > /dev/null << UNIT
[Unit]
Description=EduTech ${SVC} (java -jar)
After=network.target kafka.service edutech-auth-svc.service
Requires=network.target

[Service]
Type=simple
User=ec2-user
Environment="JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto"
EnvironmentFile=/opt/apps/.env.prod
WorkingDirectory=/opt/apps/exec-jars
ExecStart=/usr/bin/java -server -Xms256m -Xmx${HEAP_MAX} \
  -Djava.security.egd=file:/dev/urandom \
  -Dfile.encoding=UTF-8 \
  -jar ${ARTIFACT_GLOB}
StandardOutput=append:/opt/logs/${SVC}.log
StandardError=append:/opt/logs/${SVC}.log
Restart=on-failure
RestartSec=15s
TimeoutStartSec=120s

[Install]
WantedBy=multi-user.target
UNIT
  echo "  ✓ edutech-${SVC}.service (port ${PORT}, heap max ${HEAP_MAX}, artifact: ${EXT})"
}

# ─── Tomcat services (11) ─────────────────────────────────────────────────────
# ai-mentor-svc is NOT here — it has no WAR packaging, runs as java -jar
TOMCAT_SVCS=(
  auth-svc parent-svc center-svc assess-svc psych-svc
  student-profile-svc exam-tracker-svc performance-svc
  career-oracle-svc mentor-svc notification-svc
)
for SVC in "${TOMCAT_SVCS[@]}"; do
  create_tomcat_service "$SVC"
done

# ─── Exec services (4) ────────────────────────────────────────────────────────
# ai-gateway-svc: Netty exec WAR (embedded, not Tomcat)
# api-gateway + student-gateway: Spring Cloud Gateway JARs
# ai-mentor-svc: no WAR packaging — produces JAR only
create_exec_service "ai-gateway-svc"  8086 "512m" "war"
create_exec_service "ai-mentor-svc"   8093 "512m" "jar"
create_exec_service "student-gateway" 8089 "512m" "jar"
create_exec_service "api-gateway"     8180 "1g"   "jar"

# ─── Python AI sidecar ────────────────────────────────────────────────────────
sudo tee "/etc/systemd/system/edutech-python-ai-svc.service" > /dev/null << 'UNIT'
[Unit]
Description=EduTech Python AI Sidecar (FastAPI/Uvicorn)
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/python-ai/python-ai-svc
EnvironmentFile=/opt/apps/.env.prod
ExecStart=/opt/python-ai/python-ai-svc/.venv/bin/uvicorn main:app \
  --host 127.0.0.1 --port 8095 --workers 2
StandardOutput=append:/opt/logs/python-ai-svc.log
StandardError=append:/opt/logs/python-ai-svc.log
Restart=on-failure
RestartSec=10s

[Install]
WantedBy=multi-user.target
UNIT
echo "  ✓ edutech-python-ai-svc.service"

# ─── Enable all services ─────────────────────────────────────────────────────
echo
echo "=== Enabling all services (auto-start on reboot) ==="
sudo systemctl daemon-reload

ALL_SERVICES=(
  edutech-auth-svc edutech-parent-svc edutech-center-svc edutech-assess-svc
  edutech-psych-svc edutech-student-profile-svc edutech-exam-tracker-svc
  edutech-performance-svc edutech-ai-mentor-svc edutech-career-oracle-svc
  edutech-mentor-svc edutech-notification-svc
  edutech-ai-gateway-svc edutech-student-gateway edutech-api-gateway
  edutech-python-ai-svc
)

for SVC in "${ALL_SERVICES[@]}"; do
  sudo systemctl enable "$SVC" &>/dev/null && echo "  ✓ enabled: $SVC"
done

echo
echo "=== systemd services COMPLETE ==="
echo
echo "Management commands:"
echo "  sudo systemctl start   edutech-auth-svc"
echo "  sudo systemctl stop    edutech-api-gateway"
echo "  sudo systemctl restart edutech-center-svc"
echo "  sudo systemctl status  edutech-auth-svc"
echo "  journalctl -u edutech-auth-svc -f        (live logs)"
echo "  sudo systemctl start $(echo "${ALL_SERVICES[@]}" | tr ' ' '\n' | head -5 | tr '\n' ' ')..."
