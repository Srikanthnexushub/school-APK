#!/usr/bin/env bash
# =============================================================================
# EC2 Setup Step 1: Install Java 21, Kafka, system tools
# Run as ec2-user on Amazon Linux 2023
# =============================================================================
set -euo pipefail

echo "=== Step 1: System packages ==="
sudo dnf update -y -q
sudo dnf install -y java-21-amazon-corretto wget tar git nc python3.11 python3.11-pip python3.11-devel nginx -q

java -version
echo "Java 21 installed ✓"

echo "=== Step 2: Create system users ==="
id kafka &>/dev/null || sudo useradd -r -s /bin/false kafka
echo "kafka user ✓"

echo "=== Step 3: Directory structure ==="
sudo mkdir -p /opt/apps/exec-jars /opt/apps/tomcat /opt/keys /opt/logs /opt/python-ai
sudo chown -R ec2-user:ec2-user /opt/apps /opt/keys /opt/logs /opt/python-ai
echo "Directories ✓"

echo "=== Step 4: Install Kafka 3.8.0 (KRaft) ==="
KAFKA_VERSION=3.8.0
cd /opt
if [[ ! -d /opt/kafka ]]; then
  sudo wget -q "https://downloads.apache.org/kafka/${KAFKA_VERSION}/kafka_2.13-${KAFKA_VERSION}.tgz"
  sudo tar -xzf "kafka_2.13-${KAFKA_VERSION}.tgz"
  sudo mv "kafka_2.13-${KAFKA_VERSION}" kafka
  sudo rm -f "kafka_2.13-${KAFKA_VERSION}.tgz"
  sudo chown -R kafka:kafka /opt/kafka
fi

sudo mkdir -p /var/kafka-data /var/kafka-logs
sudo chown -R kafka:kafka /var/kafka-data /var/kafka-logs

# Write KRaft config
sudo tee /opt/kafka/config/kraft/server.properties > /dev/null << 'KAFKA_CONF'
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@127.0.0.1:9093
listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
advertised.listeners=PLAINTEXT://127.0.0.1:9092
inter.broker.listener.name=PLAINTEXT
controller.listener.names=CONTROLLER
log.dirs=/var/kafka-data
num.partitions=3
default.replication.factor=1
log.retention.hours=168
auto.create.topics.enable=true
num.network.threads=3
num.io.threads=8
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600
KAFKA_CONF

# Format KRaft storage (only if not already formatted)
if [[ ! -f /var/kafka-data/meta.properties ]]; then
  KAFKA_UUID=$(sudo /opt/kafka/bin/kafka-storage.sh random-uuid)
  sudo -u kafka /opt/kafka/bin/kafka-storage.sh format \
    -t "$KAFKA_UUID" \
    -c /opt/kafka/config/kraft/server.properties
  echo "Kafka storage formatted ✓"
fi

# systemd service
sudo tee /etc/systemd/system/kafka.service > /dev/null << 'KAFKA_SVC'
[Unit]
Description=Apache Kafka KRaft
After=network.target

[Service]
Type=simple
User=kafka
Environment="JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto"
ExecStart=/opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/kraft/server.properties
ExecStop=/opt/kafka/bin/kafka-server-stop.sh
Restart=on-failure
RestartSec=10s
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
KAFKA_SVC

sudo systemctl daemon-reload
sudo systemctl enable kafka
sudo systemctl start kafka
sleep 5
sudo systemctl status kafka --no-pager | grep -E "Active|running" && echo "Kafka running ✓"

echo "=== Step 5: Install mc (MinIO Client) ==="
if [[ ! -x /usr/local/bin/mc ]]; then
  sudo wget -q https://dl.min.io/client/mc/release/linux-amd64/mc -O /usr/local/bin/mc
  sudo chmod +x /usr/local/bin/mc
fi
mc --version && echo "mc installed ✓"

echo
echo "=== EC2 Setup Step 1 COMPLETE ==="
echo "   Java 21 ✓  Kafka ✓  mc ✓  Python 3.11 ✓  Nginx ✓"
