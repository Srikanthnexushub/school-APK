ARG NODE_VERSION=20

# =============================================================================
# Stage 1: Build (tsc + vite build)
# =============================================================================
FROM node:${NODE_VERSION}-alpine AS builder
WORKDIR /app

# Install dependencies with clean cache
COPY package.json package-lock.json ./
RUN npm ci --prefer-offline

# Copy source and build
COPY . .
RUN npm run build

# =============================================================================
# Stage 2: Runtime — nginx:alpine serving the SPA + proxying /api
# =============================================================================
FROM nginx:1.27-alpine AS runtime

# Non-root nginx — replace default user directive
RUN sed -i 's/user  nginx;/# user nginx; # running as non-root/g' /etc/nginx/nginx.conf

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Replace default server block with our SPA + proxy config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Remove default.conf that conflicts
RUN rm -f /etc/nginx/sites-enabled/default

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO /dev/null http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
