#!/usr/bin/env bash
# =============================================================================
# EC2 Setup Step 5: Configure Nginx for frontend SPA + API proxy
# Run as ec2-user AFTER frontend dist/ is transferred to EC2
# =============================================================================
set -euo pipefail

NGINX_CONF="/etc/nginx/conf.d/edutech.conf"
FRONTEND_DIR="/usr/share/nginx/html"

echo "=== Configuring Nginx ==="

# Remove default nginx page
sudo rm -f /etc/nginx/conf.d/default.conf

# Create EduTech nginx config
sudo tee "$NGINX_CONF" > /dev/null << 'NGINX'
# EduTech — Fortune 500 grade nginx config
upstream api_backend {
    server 127.0.0.1:8180;
    keepalive 32;
}

server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # Compression
    gzip on;
    gzip_comp_level 4;
    gzip_types application/json application/javascript text/css text/plain text/xml application/xml image/svg+xml;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_vary on;

    # Timeouts
    proxy_read_timeout     120s;
    proxy_connect_timeout  10s;
    proxy_send_timeout     60s;
    client_max_body_size   50M;

    # Static files
    location / {
        try_files $uri $uri/ /index.html;
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }
    }

    # Voice WebSocket — exact match BEFORE /api/ to use Connection: upgrade
    # /api/ uses Connection: "" for keepalive which strips the upgrade header
    location = /api/v1/ai/voice/ws {
        proxy_pass         http://api_backend;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade $http_upgrade;
        proxy_set_header   Connection "upgrade";
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_buffering    off;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }

    # API — proxied via upstream with keepalive
    location /api/ {
        proxy_pass         http://api_backend;
        proxy_http_version 1.1;
        proxy_set_header   Connection "";
        proxy_set_header   Upgrade $http_upgrade;
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_buffering    off;
    }

    # WebSocket for assess-svc real-time exams
    location /ws/ {
        proxy_pass         http://127.0.0.1:8084;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade $http_upgrade;
        proxy_set_header   Connection "upgrade";
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }

    # Health
    location /health {
        access_log off;
        return 200 "nginx-ok\n";
        add_header Content-Type text/plain;
    }

    # Security
    location ~ /\.git  { return 404; }
    location ~ /\.env  { return 404; }
    location ~ /actuator { return 404; }
}
NGINX

# Test config
sudo nginx -t && echo "Nginx config valid ✓"

# Enable and start
sudo systemctl enable nginx
sudo systemctl restart nginx
sudo systemctl status nginx --no-pager | grep -E "Active" && echo "Nginx running ✓"

echo
echo "=== Nginx setup COMPLETE ==="
echo "   Frontend served from: ${FRONTEND_DIR}"
echo "   API proxy:  /api/              → http://127.0.0.1:8180 (keepalive)"
echo "   Voice WS:   /api/v1/ai/voice/ws → http://127.0.0.1:8180 (Connection: upgrade)"
echo "   WebSocket:  /ws/               → http://127.0.0.1:8084"
echo
echo "Next: scp dist/* ec2-user@<ec2-ip>:${FRONTEND_DIR}/"
