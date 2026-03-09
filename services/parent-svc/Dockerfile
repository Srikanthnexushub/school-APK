ARG JAVA_VERSION=21
ARG MAVEN_VERSION=3.9.9

# =============================================================================
# Stage 1: Build
# =============================================================================
FROM maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION} AS builder
WORKDIR /workspace

COPY pom.xml .
COPY .mvn/ .mvn/
COPY libs/ libs/
COPY services/parent-svc/ services/parent-svc/

RUN mvn package -pl services/parent-svc --also-make -DskipTests --no-transfer-progress

# =============================================================================
# Stage 2: Runtime
# =============================================================================
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS runtime

ARG APP_VERSION=1.0.0-SNAPSHOT
ARG SVC_NAME=parent-svc

ENV APP_JAR=${SVC_NAME}-${APP_VERSION}.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
WORKDIR /app

COPY --from=builder --chown=appuser:appgroup \
  /workspace/services/parent-svc/target/${APP_JAR} app.jar

EXPOSE ${SERVER_PORT}

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
