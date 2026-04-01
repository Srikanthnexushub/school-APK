# NexusChat — Implementation Master Index

> **World-class context-aware AI chatbot for NexusEd platform.**
> Floating widget on every page. Token streaming. Proactive nudges. Action execution.

## Spec Documents

| File | Contents |
|---|---|
| [spec/01-architecture.md](spec/01-architecture.md) | System architecture, service topology, data flow diagrams |
| [spec/02-backend-service.md](spec/02-backend-service.md) | nexus-chat-svc: pom.xml, application.yml, package structure |
| [spec/03-database-migrations.md](spec/03-database-migrations.md) | Full SQL for V1–V5 Flyway migrations |
| [spec/04-java-domain.md](spec/04-java-domain.md) | All domain model classes, value objects, ports |
| [spec/05-java-application.md](spec/05-java-application.md) | All application service classes (use cases) |
| [spec/06-java-infrastructure.md](spec/06-java-infrastructure.md) | Controllers, repositories, WebClient adapters, Kafka |
| [spec/07-streaming-ai-gateway.md](spec/07-streaming-ai-gateway.md) | Changes to ai-gateway-svc for token streaming |
| [spec/08-api-contracts.md](spec/08-api-contracts.md) | Full REST API spec: request/response DTOs, endpoints |
| [spec/09-frontend-components.md](spec/09-frontend-components.md) | All React components: full TypeScript code |
| [spec/10-frontend-store-hooks.md](spec/10-frontend-store-hooks.md) | chatStore.ts, useChatStream.ts, chat types |
| [spec/11-kafka-events.md](spec/11-kafka-events.md) | Kafka consumers, producers, proactive nudge factory |
| [spec/12-config-env-gateway.md](spec/12-config-env-gateway.md) | .env additions, api-gateway.yml route, AppLayout.tsx change |
| [spec/13-implementation-waves.md](spec/13-implementation-waves.md) | Wave-by-wave build order with exact file checklist |

## Port Assignment
- **nexus-chat-svc**: `8097` (env: `NEXUS_CHAT_SVC_PORT`)

## Key Decisions
- Tomcat WAR packaging (matches 11 existing services)
- WebFlux `ResponseBodyEmitter` on Tomcat for streaming (NOT Netty)
- `application/x-ndjson` stream format (avoids WebFlux server requirement)
- Context aggregated in parallel via `Mono.zip` (≤800ms total)
- Zustand + sessionStorage (matches authStore pattern)
- One AppLayout.tsx change: add `<NexusChatWidget />` before closing div
