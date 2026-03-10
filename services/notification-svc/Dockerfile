ARG JAVA_VERSION=21
ARG MAVEN_VERSION=3.9.9

# =============================================================================
# Stage 1: Build
# =============================================================================
FROM maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION} AS builder
WORKDIR /workspace

# Copy root pom and Maven wrapper config first for layer cache optimisation
COPY pom.xml .
COPY .mvn/ .mvn/

# Copy all library modules (shared dependencies)
COPY libs/ libs/

# Copy only this service's source tree
COPY services/notification-svc/ services/notification-svc/

RUN mvn package -pl services/notification-svc --also-make -DskipTests --no-transfer-progress

# =============================================================================
# Stage 2: Runtime (minimal JRE on Alpine)
# =============================================================================
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS runtime

ARG APP_VERSION=1.0.0-SNAPSHOT
ARG SVC_NAME=notification-svc

ENV APP_JAR=${SVC_NAME}-${APP_VERSION}.jar

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
WORKDIR /app

COPY --from=builder --chown=appuser:appgroup \
  /workspace/services/notification-svc/target/${APP_JAR} app.jar

EXPOSE ${SERVER_PORT}

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
