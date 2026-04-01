# NexusChat — Backend Service Spec

## File: `services/nexus-chat-svc/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.edutech</groupId>
        <artifactId>edutech-platform</artifactId>
        <version>${revision}</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>nexus-chat-svc</artifactId>
    <packaging>war</packaging>
    <name>EduTech :: Service :: NexusChat</name>
    <description>
        Context-aware AI chatbot service. Owns: chat_sessions, chat_messages,
        context_snapshots, proactive_nudges. Streams LLM tokens via ResponseBodyEmitter.
        Port: ${NEXUS_CHAT_SVC_PORT} | DB Schema: chat_schema
    </description>

    <dependencies>
        <!-- Web (Tomcat WAR — NOT WebFlux, uses ResponseBodyEmitter for streaming) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- WebFlux WebClient for outbound calls (context aggregation) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Resilience4j — circuit breaker for ai-gateway-svc calls -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
        </dependency>

        <!-- Shared security library -->
        <dependency>
            <groupId>com.edutech</groupId>
            <artifactId>common-security</artifactId>
            <version>${revision}</version>
        </dependency>

        <!-- MapStruct for DTO mapping -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## File: `services/nexus-chat-svc/src/main/resources/application.yml`

```yaml
server:
  port: ${NEXUS_CHAT_SVC_PORT:8097}
  shutdown: graceful

spring:
  application:
    name: ${NEXUS_CHAT_SVC_NAME:nexus-chat-svc}
  threads:
    virtual:
      enabled: true

  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${NEXUS_CHAT_DB_NAME}
    username: ${NEXUS_CHAT_DB_USER}
    password: ${NEXUS_CHAT_DB_PASSWORD}
    hikari:
      maximum-pool-size: ${NEXUS_CHAT_DB_POOL_MAX_SIZE:5}
      minimum-idle: ${NEXUS_CHAT_DB_POOL_MIN_IDLE:2}
      connection-timeout: ${NEXUS_CHAT_DB_CONNECTION_TIMEOUT_MS:30000}
      idle-timeout: ${NEXUS_CHAT_DB_IDLE_TIMEOUT_MS:600000}

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: chat_schema
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    schemas: chat_schema
    locations: classpath:db/migration/chat
    baseline-on-migrate: false

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    consumer:
      group-id: ${NEXUS_CHAT_KAFKA_CONSUMER_GROUP:nexus-chat-svc-group}
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

jwt:
  public-key-path: ${JWT_PUBLIC_KEY_PATH}
  issuer: ${JWT_ISSUER}

service:
  api-key: ${SERVICE_API_KEY}

# Downstream service URIs (all from env — zero hardcoding)
downstream:
  ai-gateway:
    uri: ${AI_GATEWAY_SVC_URI}
    timeout-ms: ${NEXUS_CHAT_AI_GATEWAY_TIMEOUT_MS:30000}
  student-profile:
    uri: ${STUDENT_PROFILE_SVC_URI}
    timeout-ms: ${CTX_PROFILE_TIMEOUT_MS:500}
  performance:
    uri: ${PERFORMANCE_SVC_URI}
    timeout-ms: ${CTX_PERF_TIMEOUT_MS:500}
  ai-mentor:
    uri: ${AI_MENTOR_SVC_URI}
    timeout-ms: ${CTX_MENTOR_TIMEOUT_MS:500}
  assess:
    uri: ${ASSESS_SVC_URI}
    timeout-ms: ${CTX_ASSESS_TIMEOUT_MS:500}
  center:
    uri: ${CENTER_SVC_URI}
    timeout-ms: ${CTX_CENTER_TIMEOUT_MS:500}
  notification:
    uri: ${NOTIFICATION_SVC_URI}

context:
  total-timeout-ms: ${CTX_TOTAL_TIMEOUT_MS:800}
  cache-ttl-minutes:
    profile: ${CTX_CACHE_TTL_PROFILE_MIN:15}
    performance: ${CTX_CACHE_TTL_PERF_MIN:5}
    mentor: ${CTX_CACHE_TTL_MENTOR_MIN:10}
    assess: ${CTX_CACHE_TTL_ASSESS_MIN:10}
    center: ${CTX_CACHE_TTL_CENTER_MIN:30}

chat:
  max-history-messages: ${CHAT_MAX_HISTORY_MESSAGES:20}
  session-ttl-hours: ${CHAT_SESSION_TTL_HOURS:24}
  stream-thread-pool-size: ${CHAT_STREAM_THREAD_POOL_SIZE:10}
  proactive-inactivity-days: ${CHAT_PROACTIVE_INACTIVITY_DAYS:3}

kafka:
  topics:
    nexus-chat-events: ${KAFKA_TOPIC_NEXUS_CHAT_EVENTS:nexus-chat-events}
    notification-send: ${KAFKA_TOPIC_NOTIFICATION_SEND}
    assess-events: ${KAFKA_TOPIC_ASSESS_EVENTS}
    performance-events: ${KAFKA_TOPIC_PERFORMANCE_EVENTS}
    ai-mentor-study-plan-created: ${KAFKA_TOPIC_AI_MENTOR_STUDY_PLAN_CREATED}

resilience4j:
  circuitbreaker:
    instances:
      ai-gateway-stream:
        sliding-window-size: ${R4J_CB_AI_WINDOW_SIZE:10}
        failure-rate-threshold: ${R4J_CB_AI_FAILURE_THRESHOLD:50}
        wait-duration-in-open-state: ${R4J_CB_AI_WAIT_DURATION:30s}
        permitted-number-of-calls-in-half-open-state: 3

management:
  endpoints:
    web:
      exposure:
        include: ${ACTUATOR_ENDPOINTS:health,info,prometheus,metrics}
  metrics:
    tags:
      application: ${NEXUS_CHAT_SVC_NAME:nexus-chat-svc}
      environment: ${APP_ENVIRONMENT:local}
  tracing:
    sampling:
      probability: ${OTEL_SAMPLING_PROBABILITY:1.0}

otel:
  exporter:
    otlp:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}

logging:
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    com.edutech: ${LOG_LEVEL_APP:DEBUG}

loki:
  url: ${LOKI_URL:http://localhost:3100}
log:
  dedup:
    allowed-repetitions: ${LOG_DEDUP_ALLOWED_REPETITIONS:5}
    cache-size: ${LOG_DEDUP_CACHE_SIZE:500}
```

## Package Structure

```
services/nexus-chat-svc/src/main/java/com/edutech/chat/
├── NexusChatApplication.java
├── domain/
│   ├── model/
│   │   ├── ChatSession.java
│   │   ├── ChatMessage.java
│   │   ├── MessageRole.java           (enum: USER, ASSISTANT, SYSTEM)
│   │   ├── MessageType.java           (enum: TEXT, ACTION_RESULT, CONTEXT_CARD)
│   │   ├── SessionStatus.java         (enum: ACTIVE, ARCHIVED)
│   │   ├── StudentContext.java        (record — assembled by ContextEngine)
│   │   ├── WeakAreaSummary.java       (record — from performance-svc)
│   │   ├── MasterySummary.java        (record — from performance-svc)
│   │   ├── StudyPlanSummary.java      (record — from ai-mentor-svc)
│   │   ├── RecentExamSummary.java     (record — from assess-svc)
│   │   ├── ProactiveNudge.java
│   │   ├── NudgeTriggerType.java      (enum)
│   │   └── ActionCommand.java         (record — parsed from AI response)
│   ├── port/
│   │   ├── in/
│   │   │   ├── StartSessionUseCase.java
│   │   │   ├── SendMessageUseCase.java
│   │   │   ├── StreamMessageUseCase.java
│   │   │   ├── GetSessionHistoryUseCase.java
│   │   │   ├── DeleteSessionUseCase.java
│   │   │   ├── ExecuteActionUseCase.java
│   │   │   └── GetPendingNudgesUseCase.java
│   │   └── out/
│   │       ├── ChatSessionRepository.java
│   │       ├── ChatMessageRepository.java
│   │       ├── ProactiveNudgeRepository.java
│   │       ├── ContextSnapshotRepository.java
│   │       ├── AiGatewayStreamPort.java
│   │       ├── ContextAggregatorPort.java
│   │       └── ChatEventPublisherPort.java
│   └── event/
│       ├── ChatSessionStartedEvent.java
│       ├── ChatMessageSentEvent.java
│       └── ProactiveNudgeTriggeredEvent.java
├── application/
│   └── service/
│       ├── ChatSessionService.java
│       ├── ContextAggregatorService.java
│       ├── SystemPromptBuilder.java
│       ├── ActionExecutorService.java
│       └── ProactiveNudgeService.java
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   ├── ChatController.java
    │   │   └── kafka/
    │   │       └── PlatformEventKafkaConsumer.java
    │   └── out/
    │       ├── jpa/
    │       │   ├── JpaChatSessionRepository.java
    │       │   ├── JpaChatMessageRepository.java
    │       │   ├── JpaProactiveNudgeRepository.java
    │       │   └── JpaContextSnapshotRepository.java
    │       ├── webclient/
    │       │   ├── AiGatewayStreamWebClientAdapter.java
    │       │   ├── StudentProfileWebClientAdapter.java
    │       │   ├── PerformanceWebClientAdapter.java
    │       │   ├── AiMentorWebClientAdapter.java
    │       │   ├── AssessWebClientAdapter.java
    │       │   └── CenterWebClientAdapter.java
    │       └── kafka/
    │           └── KafkaChatEventPublisher.java
    └── config/
        ├── WebClientConfig.java
        ├── SecurityConfig.java
        ├── KafkaConfig.java
        ├── StreamThreadPoolConfig.java
        └── DownstreamProperties.java
```

## File: `NexusChatApplication.java`

```java
package com.edutech.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class NexusChatApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(NexusChatApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NexusChatApplication.class, args);
    }
}
```
