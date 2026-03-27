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
# EduTech AI Platform — Nginx reverse proxy + SPA
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    # Frontend SPA
    root /usr/share/nginx/html;
    index index.html;

    # Increase timeouts for long-running AI requests
    proxy_read_timeout     120s;
    proxy_connect_timeout  10s;
    proxy_send_timeout     60s;

    # Increase body size for file uploads (MinIO/S3 presigned via center-svc)
    client_max_body_size 50M;

    # SPA routing — serve index.html for all non-asset routes
    location / {
        try_files $uri $uri/ /index.html;
        # Cache static assets aggressively
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }
    }

    # API Gateway proxy — all /api/ routes
    location /api/ {
        proxy_pass         http://127.0.0.1:8180;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade $http_upgrade;
        proxy_set_header   Connection 'upgrade';
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
        proxy_read_timeout 3600s;   # keep WS alive during exam
        proxy_send_timeout 3600s;
    }

    # Health check endpoint (for load balancer / monitoring)
    location /health {
        access_log off;
        return 200 "nginx-ok\n";
        add_header Content-Type text/plain;
    }

    # Block common attack vectors
    location ~ /\.git { return 404; }
    location ~ /\.env { return 404; }
    location ~ /actuator { return 404; }   # actuator only via internal ports
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
echo "   API proxy:  /api/ → http://127.0.0.1:8180"
echo "   WebSocket:  /ws/  → http://127.0.0.1:8084"
echo
echo "Next: scp dist/* ec2-user@<ec2-ip>:${FRONTEND_DIR}/"
