
🎓 EDUTECH AI PLATFORM
Enterprise-Grade Microservices Architecture
Fortune 500 Level · SLA 99.99% · AI-First Design


☁️ Cloud Native
🤖 AI-Powered
🔐 Zero Trust
📱 Mobile First
🚀 99.99% SLA



CLAUDE CODE MASTER PROMPT DOCUMENT
Version 1.0  |  Stack: Java Spring Boot · React · PostgreSQL · Docker/K8s · Argon2id


1. PROJECT OVERVIEW & VISION

EduTech AI Platform is an enterprise-grade, microservices-based educational technology system designed for coaching centers, parents, and students. Every module is AI-augmented, cloud-native, and built to Fortune 500 production standards. The system targets 99.99% SLA, sub-100ms API latency on the critical path, and full mobile delivery via React Native APK.

🎯 Core Mission
→  Digitize and intelligently automate coaching center operations end-to-end
→  Provide AI-driven psychometric profiling for personalized student growth
→  Deliver real-time assessment, adaptive grading, and parent transparency
→  Ship as Android APK (React Native) with offline-first capability
→  Achieve Fortune-level security, observability, and resilience from Day 1

1.1  Unique AI Features (Beyond Standard Implementations)
•	🧠 Adaptive Learning Engine — per-student neural difficulty calibration (IRT + Bayesian)
•	🎙️ Voice-to-Assessment — speech NLP converts spoken answers to graded text
•	👁️ Proctoring AI — computer vision anomaly detection during online tests
•	🔮 Dropout Predictor — LSTM model forecasting student disengagement 30 days ahead
•	🧬 Psychometric DNA — multi-axis trait mapping (MBTI + Big5 + learning style fusion)
•	💬 Parent Copilot — LLM chatbot summarizing child's week, flagging issues in native language
•	📊 Smart Grade Normalizer — cross-batch AI grade curve with bias detection
•	🗺️ Career Path Oracle — AI career trajectory from psychometric + academic data
•	🔍 Plagiarism & Integrity Guard — semantic similarity + keystroke dynamics analysis
•	⚡ Real-time Doubt Resolver — RAG-powered Q&A using coaching center's own content

1.2  Technology Stack
Layer
Technology
Rationale
Backend Core
Java 21 + Spring Boot 3.3 (Virtual Threads)
Record-breaking throughput with Loom
Security
Spring Security 6 + Argon2id + JWT (RS256)
OWASP Top-10 compliant, memory-hard hashing
Database
PostgreSQL 16 + pgvector + TimescaleDB
ACID + vector search + time-series metrics
Caching
Redis 7 Cluster + Caffeine L1
Two-tier: in-process + distributed
Message Bus
Apache Kafka 3.6 (KRaft mode)
Event sourcing, audit log, async decoupling
API Gateway
Spring Cloud Gateway + Rate Limiter
Circuit breaker, retry, JWT validation edge
Service Mesh
Istio + Envoy sidecar
mTLS, traffic policy, canary deployments
Frontend
React 18 + Vite + TailwindCSS
Concurrent rendering, fast HMR, utility CSS
Mobile
React Native 0.74 + Expo (managed)
Single codebase → APK + IPA + Web
AI/ML
Python FastAPI sidecar + OpenAI / Ollama
Polyglot AI; swap providers without recompile
Observability
OpenTelemetry → Grafana LGTM stack
Logs, metrics, traces, profiles in one pane
Container Orchestration
Kubernetes 1.30 + Helm 3 + ArgoCD
GitOps, auto-scaling, blue-green rollouts
CI/CD
GitHub Actions + Trivy + SBOM
Shift-left security, attestation, SLSA Level 3
IDE
IntelliJ IDEA Ultimate + Sonarlint
Real-time static analysis and code quality


2. MICROSERVICES ARCHITECTURE

All services follow Hexagonal Architecture (Ports & Adapters), DDD aggregates, CQRS with event sourcing on write side, and expose REST + gRPC dual interfaces. Each service is immutable-deployment-ready (no mutable state in pods), packaged as a distroless container image.

2.1  Authentication Service  [auth-svc]
📦 Service Identity
Port: 8081 | gRPC: 9091 | DB Schema: auth_schema
Owns: users, roles, permissions, sessions, captcha_tokens, otp_registry
Image: gcr.io/edutech/auth-svc:latest (distroless Java 21)

RBAC Model — Level 14 Granularity
•	Role Hierarchy: SUPER_ADMIN → CENTER_ADMIN → TEACHER → PARENT → STUDENT → GUEST
•	Permission Matrix: Resource × Action × Scope (global / center / batch / self)
•	Attribute-Based Access Control (ABAC) overlay for time-windowed permissions
•	Dynamic Role Assignment via JWT claims with short TTL (15 min access, 7-day refresh)
•	Immutable audit log for every permission change (append-only Kafka topic)

AI Features in Auth
•	🤖 Behavioral Biometrics — typing cadence + mouse dynamics ML anomaly score
•	🚨 Zero-Day Threat Detection — LSTM on login patterns to flag credential stuffing
•	🌐 GeoIP Risk Scoring — impossible travel detection with Haversine formula
•	🎯 Adaptive CAPTCHA — risk score drives puzzle complexity (hCaptcha Enterprise API)

Implementation Specifics
// Argon2id config (OWASP recommended 2024)
Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
.withMemoryCost(65536)   // 64 MB
.withParallelism(2)
.withIterations(3)
.withSaltLength(16)
.withHashLength(32);

// JWT RS256 with rotating keys (JWKS endpoint)
// Access token: 15 min | Refresh token: 7 days (Redis backed)
// Device fingerprint bound refresh tokens

Email & SMS Verification
•	OTP: 6-digit TOTP (RFC 6238) with 5-min expiry, max 3 attempts
•	Email: AWS SES / SendGrid with DKIM + DMARC signing
•	SMS: Twilio Verify API with fallback to SNS
•	Magic Link: HMAC-SHA256 signed, single-use, 10-min TTL
•	Immutable: OTP tokens stored in Redis with TTL; DB record is write-once

2.2  Parent Service  [parent-svc]
📦 Service Identity
Port: 8082 | gRPC: 9092 | DB Schema: parent_schema
Owns: parent_profiles, child_links, notifications, daily_reports, consent_records
Depends on: auth-svc (token validation), assessment-svc (grades), psych-svc (traits)

Core Capabilities
•	Child Performance Dashboard — real-time grades, attendance, test history
•	Push Notification Engine — FCM / APNs via Firebase Admin SDK
•	Consent Management — COPPA/DPDP compliant digital consent with versioning
•	Multi-Child Support — single parent linked to N children across M coaching centers
•	Fee & Payment Visibility — read-only view of invoices from coaching-center-svc

AI Features in Parent Service
•	💬 Parent Copilot — weekly AI summary of child's performance in parent's language (i18n + LLM)
•	📈 Progress Trajectory — regression model predicting child's rank at next exam
•	🔔 Smart Alert Engine — ML prioritizes alerts to avoid notification fatigue
•	🧒 Wellbeing Radar — sentiment analysis on child's self-assessments, alerts parent if distress
•	🗣️ Natural Language Reports — 'How is my child doing?' → LLM queries internal APIs → narrative response

2.3  Coaching Center Service  [center-svc]
📦 Service Identity
Port: 8083 | gRPC: 9093 | DB Schema: center_schema
Owns: centers, batches, teachers, schedules, fee_structures, attendance, content_library
Event producer: BATCH_CREATED, SCHEDULE_CHANGED, TEACHER_ASSIGNED, CONTENT_UPLOADED

Core Capabilities
•	Multi-Tenant Architecture — center isolation via row-level security (RLS) in PostgreSQL
•	Batch & Schedule Management — drag-and-drop scheduler with conflict detection
•	Teacher Workload Balancing — auto-assign based on subject expertise + load score
•	Content Library — versioned study material with CDN delivery (S3 + CloudFront)
•	Fee Engine — flexible plans (monthly/quarterly/annual), discount rules, GST computation
•	Attendance — QR code + face recognition check-in (optional AI upgrade)

AI Features in Coaching Center Service
•	📅 AI Timetable Generator — constraint satisfaction optimizer for schedule conflicts
•	📚 Content Recommender — collaborative filtering surfaces relevant material per student
•	👨‍🏫 Teacher Performance Index — AI score from student outcomes + feedback + attendance
•	🔍 Doubt Resolver RAG — LLM + pgvector over center's own content library
•	📉 Batch Health Monitor — early warning system when batch average drops > 2σ
•	💡 Fee Defaulter Predictor — payment delay prediction 30 days in advance

2.4  Assessment & Grading Service  [assess-svc]
📦 Service Identity
Port: 8084 | gRPC: 9094 | DB Schema: assess_schema
Owns: exams, questions, submissions, grades, analytics, proctoring_sessions
Uses: TimescaleDB for time-series performance data; pgvector for question embeddings

Core Capabilities
•	Question Bank — tagged by topic, difficulty (IRT b-param), bloom level, exam type
•	Adaptive Test Engine — CAT (Computer Adaptive Testing) using Item Response Theory
•	Multi-format Questions — MCQ, fill-blank, code execution, essay, voice answer
•	Secure Exam Mode — token-bound sessions, tab-switch detection, copy-paste block
•	Instant Grading — automated for objective; AI-assisted for subjective
•	Grade Analytics — percentile, z-score, batch comparison, historical trend

AI Features in Assessment & Grading
•	🎯 IRT-based Adaptive CAT — real-time theta estimation, optimal next question selection
•	🗣️ Voice Answer Grading — Whisper STT → NLP semantic scoring against rubric
•	👁️ AI Proctoring — YOLO object detection (phone, book), gaze tracking, audio analysis
•	✍️ Essay Auto-Grader — BERT fine-tuned on domain rubrics, returns score + feedback
•	🔍 Plagiarism Guard — sentence embedding cosine similarity + keystroke dynamics
•	📊 Bias-Free Grade Normalizer — statistical AI curve with demographic fairness audit
•	❓ Auto Question Generator — LLM generates distractors + answer key from any PDF

2.5  Psychometric Service  [psych-svc]  — Coaching Center Exclusive
📦 Service Identity
Port: 8085 | gRPC: 9095 | DB Schema: psych_schema
Owns: psych_profiles, trait_dimensions, career_mappings, session_histories
AI Dependency: Python FastAPI sidecar (psych-ai-svc:8095) for heavy ML inference

Core Capabilities
•	Multi-Framework Profiling — Big Five (OCEAN), MBTI type, VARK learning style, Holland Code
•	Adaptive Questionnaire — branching logic, IRT-calibrated items, 25-40 min session
•	Longitudinal Tracking — profile evolution every semester, delta analysis
•	Counselor Dashboard — visual radar charts, session notes, intervention flags
•	Career Mapping — O*NET alignment, skill gap analysis, subject recommendation

AI Features in Psychometric Service
•	🧬 Psychometric DNA — fusion model: trait × academic × behavioral = 360° student profile
•	🔮 Career Path Oracle — gradient boosted trees on 10K+ career trajectories dataset
•	🎭 Personality Drift Detector — time-series anomaly detection on trait scores
•	🧩 Team Composition AI — optimize study groups using personality compatibility scores
•	💊 Intervention Recommender — ML matches student profile to proven support strategies
•	🌐 Multilingual Assessment — question translation + response analysis in 12 Indian languages


3. INFRASTRUCTURE & SLA ARCHITECTURE

3.1  SLA 99.99% Design Patterns
Pattern
Implementation
Target Metric
Circuit Breaker
Resilience4j: 50% failure threshold, 30s half-open
Fault isolation < 100ms
Rate Limiting
Redis token bucket: 1000 RPS/tenant, 10k global
Throttle before saturation
Retry + Backoff
Exponential jitter: 3 retries, max 8s
P99 success on transient
Bulkhead
Thread pool isolation per downstream
No cascade failure
Health Probes
Liveness + Readiness + Startup probes
Zero traffic to sick pods
HPA + KEDA
CPU 60% → scale out; Kafka lag → scale workers
Auto-scale in 45s
PodDisruptionBudget
Min available = 2 replicas per service
Zero-downtime deploys
Database HA
Postgres Patroni: 1 primary + 2 replicas + Pgpool
RPO 0s, RTO < 30s
CDN + Edge Cache
CloudFront + Redis: static assets + API responses
< 20ms TTFB global
Chaos Engineering
Chaos Monkey weekly + Gremlin DR drills
Validated resilience

3.2  Observability Stack (LGTM)
•	Loki — structured JSON log aggregation with tenant labels
•	Grafana — 40+ pre-built dashboards per service + SLO burn-rate alerts
•	Tempo — distributed tracing with trace-to-log correlation
•	Mimir — high-cardinality Prometheus metrics, 90-day retention
•	OpenTelemetry SDK — auto-instrumentation for all Spring Boot services
•	Alertmanager — PagerDuty escalation for P1; Slack for P2/P3

3.3  Security Architecture (Zero Trust)
•	mTLS between all services via Istio PeerAuthentication (STRICT mode)
•	JWKS-based token validation at API Gateway (no secret sharing between services)
•	Secrets managed by HashiCorp Vault with dynamic DB credentials (15-min lease)
•	Container image scanning: Trivy in CI + Falco runtime threat detection
•	Network Policy: default-deny-all; explicit allow-list per service pair
•	OWASP dependency check + Dependabot auto-PRs for CVE remediation
•	RBAC on Kubernetes: least-privilege ServiceAccounts per pod
•	Audit logging: all admin actions → immutable Kafka topic → S3 Glacier


4. IMPLEMENTATION STAGES (Stage-by-Stage Plan)

Each stage is time-boxed. Frontend React and React Native are developed in parallel at every stage. Test cases (JUnit 5 + Testcontainers + Playwright) must pass before stage sign-off.

STAGE 1 — DESIGN  (Week 1–2)
Stage 1 Deliverables
✅  Domain model diagrams (PlantUML / Mermaid)
✅  API contract first (OpenAPI 3.1 specs for all 5 services)
✅  Database ERD (dbdiagram.io export + SQL DDL with RLS policies)
✅  Sequence diagrams: auth flow, exam flow, psych assessment flow
✅  Wireframes + Design System (Figma: colors, typography, component library)
✅  ADRs (Architecture Decision Records) in /docs/adr/
✅  Threat model (STRIDE) for auth and assessment services

Stage 1 — Claude Code Prompt
Generate complete OpenAPI 3.1 YAML specifications for auth-svc, parent-svc,
center-svc, assess-svc, and psych-svc. Include: all endpoints, request/response
schemas with validation constraints (JSR-380), error envelopes (RFC 7807),
security schemes (BearerAuth + ApiKeyAuth), and rate-limit response headers.
Also generate PostgreSQL DDL for each service schema with:
- UUID primary keys, created_at/updated_at/deleted_at (soft delete),
- Row-Level Security policies for multi-tenancy,
- pgvector extension for AI embedding columns,
- Proper indexes (GIN for JSONB, BRIN for timestamps, B-tree composite).

STAGE 2 — FOUNDATION  (Week 3–5)
Stage 2 Deliverables
✅  Mono-repo structure: /services/{auth,parent,center,assess,psych}-svc
✅  Spring Boot 3.3 parent POM with shared BOM (dependency versions locked)
✅  Shared libraries: /libs/{common-security, event-contracts, test-fixtures}
✅  Docker Compose (local dev) + Helm charts skeleton
✅  CI pipeline: GitHub Actions → build → test → Trivy scan → push
✅  Database migrations: Flyway V1 scripts per service
✅  Auth Service fully operational: RBAC, Argon2id, JWT RS256, CAPTCHA, OTP
✅  React app scaffolded: Vite + TailwindCSS + React Router + React Query
✅  React Native Expo project initialized with navigation skeleton

Stage 2 — Claude Code Prompt
Implement the auth-svc Spring Boot 3.3 service with Java 21 virtual threads.
Requirements:
1. Spring Security 6 RBAC with 6-level role hierarchy
2. Argon2id password hashing (65536 memory, 3 iterations, parallelism 2)
3. JWT RS256: 15-min access token + 7-day refresh (Redis backed, device-bound)
4. TOTP OTP (RFC 6238): 6-digit, 5-min TTL, max 3 attempts, Redis storage
5. hCaptcha Enterprise verification endpoint
6. Behavioral biometrics risk scoring (typing cadence API endpoint)
7. Immutable audit log via Kafka producer (append-only)
8. All DTOs use Java Records; entities are @Immutable where applicable
9. Global exception handler with RFC 7807 ProblemDetail
10. Full test coverage: unit (Mockito), integration (Testcontainers + PostgreSQL)
    Generate: entity classes, repositories, service layer, REST controllers,
    security config, Flyway migrations, and Docker Compose config.

STAGE 3 — CODE  (Week 6–12)
Stage 3 Deliverables
✅  All 5 microservices fully implemented with AI feature integration
✅  API Gateway (Spring Cloud Gateway) with JWT validation, rate limiting, routing
✅  Kafka event bus: topics, producers, consumers per service
✅  Python AI sidecar: FastAPI endpoints for ML inference (psych, proctoring, grading)
✅  React frontend: all pages with React Query data fetching + optimistic updates
✅  React Native: auth flow, dashboard, assessment player, notifications
✅  E2E tests: Playwright (web) + Detox (mobile)
✅  Performance baseline: Gatling load tests (1000 concurrent users, p99 < 200ms)

Stage 3 — Claude Code Prompts by Service
── ASSESS-SVC PROMPT ──────────────────────────────────────────────────────
Implement the assessment-svc with Computer Adaptive Testing (IRT-CAT):
1. Question entity with IRT parameters (a-discrimination, b-difficulty, c-guessing)
2. CAT algorithm: theta estimation via Newton-Raphson, Fisher information item selection
3. WebSocket endpoint for real-time exam session (STOMP over SockJS)
4. Proctoring: event stream processor (tab-switch, focus-loss) → risk score
5. AI grading pipeline: async Kafka consumer → Python sidecar → grade publish
6. TimescaleDB hypertable for student_performance_ts (partitioned by week)
7. pgvector cosine search for 'find similar questions' feature
8. Immutable submission records: once submitted, no UPDATE allowed (DB constraint)

── PSYCH-SVC PROMPT ───────────────────────────────────────────────────────
Implement the psych-svc + Python FastAPI AI sidecar:
1. Adaptive questionnaire engine: branching JSON DSL stored in PostgreSQL JSONB
2. Big Five scoring: sum-scoring with reverse items, norm tables by age group
3. Python sidecar: scikit-learn pipeline for career prediction (RandomForest + SHAP)
4. pgvector storage for psychometric trait embeddings (for similarity search)
5. REST endpoint: POST /api/v1/psych/career-match → top 5 careers with confidence
6. Longitudinal delta endpoint: GET /api/v1/psych/trajectory/{studentId}
7. gRPC endpoint: PsychService.GetProfile(StudentId) for inter-service calls

STAGE 4 — HARDENING  (Week 13–15)
Stage 4 Deliverables
✅  Security hardening: DAST (OWASP ZAP), dependency audit, secrets rotation
✅  Performance tuning: HikariCP pool sizing, Kafka consumer group optimization
✅  Immutability audit: all domain events @Immutable, Kafka topics retention = forever
✅  GDPR/DPDP compliance: right-to-erasure flow, consent audit trail
✅  Chaos engineering: Chaos Monkey + network partition tests
✅  DB query optimization: EXPLAIN ANALYZE on all N+1 risks, add missing indexes
✅  APK build: React Native Expo EAS Build → .apk + Play Store bundle (.aab)
✅  Accessibility audit: WCAG 2.1 AA for web; TalkBack/VoiceOver for mobile

Stage 4 — Claude Code Prompt
Perform hardening across all 5 services:
1. Add @Immutable annotation to all domain event classes; enforce via ArchUnit test
2. Implement data erasure saga: GDPR delete request → async saga across all schemas
3. Add Resilience4j annotations: @CircuitBreaker, @RateLimiter, @Retry, @Bulkhead
   on all inter-service calls with fallback methods
4. Optimize all JPA queries: add @EntityGraph to avoid N+1, use projections for lists
5. Add Spring Cache @Cacheable with Caffeine L1 + Redis L2 on hot read paths
6. Implement OWASP security headers filter (CSP, HSTS, X-Frame-Options)
7. Write Gatling simulation targeting 1000 RPS sustained for 10 minutes
8. Generate SBOM (CycloneDX) via Maven plugin and attest with sigstore

STAGE 5 — SHIPPING  (Week 16–18)
Stage 5 Deliverables
✅  Kubernetes manifests: Deployments, Services, HPA, PDB, NetworkPolicy, Ingress
✅  Helm charts: parameterized per environment (dev/staging/prod)
✅  ArgoCD application manifests + ApplicationSet for multi-env GitOps
✅  React build: Docker multi-stage → Nginx → K8s Deployment
✅  APK: Expo EAS Build production APK signed with keystore
✅  Monitoring: Grafana dashboards imported, SLO alerts active, PagerDuty linked
✅  Runbook: incident response SOP, rollback procedure, disaster recovery drill log
✅  Documentation: Swagger UI live, Storybook for React components, API changelog

Stage 5 — Claude Code Prompt
Generate complete Kubernetes + Helm configuration for production deployment:
1. Deployment.yaml per service: 3 replicas, resource requests/limits, probes,
   anti-affinity rules (one pod per node), terminationGracePeriodSeconds: 60
2. HPA: CPU 60% + memory 70% triggers; min=2, max=20 replicas
3. PodDisruptionBudget: minAvailable=2 per service
4. NetworkPolicy: default-deny-all ingress/egress + explicit allow rules
5. Istio VirtualService: canary split 95/5 for new deployments
6. ArgoCD Application CRDs pointing to /helm/charts/* in GitOps repo
7. Prometheus ServiceMonitor + Grafana dashboard JSON for each service
8. Expo EAS Build config (eas.json) for production APK with keystore setup
   and OTA update configuration via expo-updates


5. FRONTEND — REACT WEB + REACT NATIVE APK

5.1  React Web (Parallel at Each Stage)
Built with React 18, Vite 5, TailwindCSS 3, React Query v5, React Router v6, Zustand for global state. All components follow atomic design.

User Journeys — Frozen & Immutable
Role
Primary Journey
AI Enhancement
Student
Login → Dashboard → Take Exam → View Results → Doubt Chat
Adaptive CAT + Voice Answer + Proctoring overlay
Parent
Login → Child Overview → Reports → Copilot Chat → Fee View
LLM weekly summary + Smart alert triage
Teacher
Login → Batch View → Create Exam → Grade Dashboard → Insights
Auto question gen + AI grade assist + Batch health
Center Admin
Login → Operations Hub → Staff Mgmt → Analytics → Finance
Timetable AI + Fee predictor + Teacher performance
Super Admin
Login → All Centers → System Health → Audit Log → Config
Anomaly detection + Usage forecasting + Billing AI

5.2  React Native APK — Expo EAS Build
•	Expo SDK 51 managed workflow — single codebase for Android APK + iOS IPA
•	Navigation: Expo Router (file-based routing, same as Next.js pattern)
•	Offline First: React Query + MMKV (JSI-based) for persisted offline cache
•	Biometric Auth: expo-local-authentication (fingerprint / Face ID)
•	Push: Expo Notifications + Firebase Cloud Messaging
•	Secure Storage: expo-secure-store for token keychain storage
•	OTA Updates: expo-updates with critical-update forcing
•	Build: eas build --platform android --profile production → signed .apk + .aab

5.3  Test Coverage Requirements
Layer
Framework
Coverage Target
Key Scenarios
Backend Unit
JUnit 5 + Mockito
≥ 85% line coverage
Service logic, validators, mappers
Backend Integration
Testcontainers + PG
All API endpoints
DB, Kafka, Redis, full stack
Contract
Spring Cloud Contract
All service interfaces
Consumer-driven CDC tests
React Web E2E
Playwright
Critical user journeys
Auth, exam, dashboard, parent copilot
React Native
Detox + Jest
Navigation + key flows
Login, exam player, push notif
Performance
Gatling
1000 RPS / p99 < 200ms
Auth, exam submit, grade fetch
Security
OWASP ZAP + Trivy
Zero critical CVEs
DAST scan all endpoints


6. MASTER CLAUDE CODE PROMPT  (Copy-Paste Ready)

⚠️  Usage Instructions
1. Paste this prompt at the START of every Claude Code session for this project.
2. Append the stage-specific prompt from Section 4 after this master context.
3. Always include the relevant OpenAPI spec file path in your follow-up message.
4. Use claude-sonnet-4-6 model. Set max_tokens: 8192 for code generation.

═══════════════════════════════════════════════════════════════════════════
EDUTECH AI PLATFORM — MASTER CONTEXT PROMPT FOR CLAUDE CODE (Sonnet 4.6)
═══════════════════════════════════════════════════════════════════════════

## PROJECT IDENTITY
Project: EduTech AI Platform
Type: Enterprise-grade, AI-first educational technology platform
Architecture: Microservices (Hexagonal / DDD / CQRS + Event Sourcing)
SLA Target: 99.99% uptime (< 52 min downtime/year)
Security Level: Fortune 500 / OWASP Top-10 compliant / Zero Trust
Immutability: ALL domain events, OTP records, audit logs are IMMUTABLE.
Use Java Records for DTOs. Never mutate domain events.

## TECH STACK (STRICT — DO NOT DEVIATE)
Backend:
- Java 21 (Virtual Threads / Project Loom enabled)
- Spring Boot 3.3.x with Spring Security 6
- Spring Data JPA (Hibernate 6) + Spring Data Redis
- Spring Cloud Gateway (API Gateway)
- Spring Cloud Contract (CDC testing)
- Apache Kafka 3.6 (KRaft, no ZooKeeper)
- PostgreSQL 16 (+ pgvector + TimescaleDB extensions)
- Redis 7 Cluster (Lettuce client)
- Flyway for DB migrations
- MapStruct for DTO mapping (compile-time, zero reflection)
- Resilience4j for fault tolerance patterns

Security:
- Password hashing: Argon2id ONLY
  (memory=65536, iterations=3, parallelism=2, saltLen=16, hashLen=32)
- JWT: RS256 asymmetric signing via JWKS endpoint
- Access token TTL: 15 minutes | Refresh token TTL: 7 days
- Refresh tokens: Redis-backed, device-fingerprint-bound, single-use rotation
- NEVER use HS256, NEVER store JWT secret in application.properties
- CAPTCHA: hCaptcha Enterprise (NOT reCAPTCHA)
- OTP: 6-digit TOTP (RFC 6238), Redis TTL 300s, max 3 attempts

Frontend:
- React 18 + Vite 5 + TypeScript strict mode
- TailwindCSS 3 (no CSS modules, no styled-components)
- React Query v5 (@tanstack/react-query) for all server state
- Zustand for client-only global state
- React Router v6 (createBrowserRouter pattern)
- Zod for runtime schema validation on all API responses
- Playwright for E2E tests

Mobile:
- React Native 0.74 + Expo SDK 51 (managed workflow)
- Expo Router (file-based navigation)
- expo-secure-store for token storage
- MMKV (react-native-mmkv) for offline cache
- EAS Build for APK production build

Infrastructure:
- Docker (distroless Java 21 base images, multi-stage builds)
- Kubernetes 1.30 + Helm 3 + ArgoCD (GitOps)
- Istio service mesh (mTLS STRICT between all services)
- OpenTelemetry (OTLP) → Grafana LGTM stack
- HashiCorp Vault for secrets (dynamic DB credentials, 15-min lease)
- GitHub Actions CI: build → test → Trivy scan → SBOM → push → deploy

## MICROSERVICES
1. auth-svc       (port 8081 / gRPC 9091) — RBAC, Auth, OTP, CAPTCHA
2. parent-svc     (port 8082 / gRPC 9092) — Parent portal, Child links, Copilot
3. center-svc     (port 8083 / gRPC 9093) — Multi-tenant coaching center mgmt
4. assess-svc     (port 8084 / gRPC 9094) — CAT exams, grading, proctoring
5. psych-svc      (port 8085 / gRPC 9095) — Psychometric profiling, career AI
6. ai-gateway-svc (port 8086)              — Routes to Python FastAPI ML sidecars
7. api-gateway    (port 8080)              — Spring Cloud Gateway, edge layer

## CODING STANDARDS (MANDATORY)
- SOLID principles: Single Responsibility strictly enforced per class
- Package structure: {service}.domain | .application | .infrastructure | .api
- DTOs: Java Records ONLY (immutable by default)
- Entities: JPA @Entity with @Version for optimistic locking
- Error handling: GlobalExceptionHandler → RFC 7807 ProblemDetail
- Logging: SLF4J structured JSON (Logback) with MDC trace/span IDs
- No field injection (@Autowired on fields) — constructor injection ONLY
- No Lombok (use Java Records + IDE generation for entities)
- All public APIs documented with @Operation (SpringDoc OpenAPI 3.1)
- ArchUnit tests enforce layered dependency rules
- Minimum test coverage: 85% line, 80% branch

## AI INTEGRATION PATTERN
- AI calls always go through ai-gateway-svc (circuit-breaker protected)
- AI responses are NEVER trusted blindly: validate with Zod/Bean Validation
- Async AI processing: request → Kafka → Python sidecar → Kafka → consumer
- AI model: default to claude-sonnet-4-6 via Anthropic API OR Ollama (local)
- Embeddings: text-embedding-3-small (1536-dim) stored in pgvector columns
- RAG pattern: query → embed → pgvector cosine search → augment → LLM

## RESPONSE FORMAT FOR CODE GENERATION
When generating code, ALWAYS provide:
1. Complete, compilable file (no '// TODO implement' stubs)
2. File path comment at top: // src/main/java/com/edutech/{pkg}/{Class}.java
3. Corresponding test class in src/test/java/
4. Flyway migration SQL if entity changes are involved
5. OpenAPI annotation if it's a REST controller
6. Mention any new Maven/npm dependency to add

## IMMUTABILITY CHECKLIST (verify before generating any class)
☐ Domain Events → Java Record (cannot be mutated after creation)
☐ OTP/Token records → @Column(updatable=false) on all fields
☐ Audit log entries → insert-only (no UPDATE in repository)
☐ Exam submissions → @PreUpdate throws exception after SUBMITTED status
☐ Kafka messages → Avro schema with schema registry (schema evolution rules)

## CURRENT STAGE
<< REPLACE THIS LINE WITH: STAGE 1 / 2 / 3 / 4 / 5 + specific task >>

## TASK
<< REPLACE THIS LINE WITH YOUR SPECIFIC IMPLEMENTATION REQUEST >>

═══════════════════════════════════════════════════════════════════════════
END OF MASTER PROMPT — Append stage-specific prompt below this line
═══════════════════════════════════════════════════════════════════════════


7. APK BUILD PIPELINE

7.1  React Native + Expo EAS Build → Signed APK
The mobile app shares all business logic via shared TypeScript packages in the monorepo. Screens are mobile-optimized versions of the React web pages.

EAS Build Configuration
// eas.json
{
"cli": { "version": ">= 10.0.0" },
"build": {
"development": {
"developmentClient": true,
"distribution": "internal",
"android": { "buildType": "apk" }
},
"preview": {
"distribution": "internal",
"android": { "buildType": "apk" }
},
"production": {
"android": {
"buildType": "app-bundle",
"credentialsSource": "remote"
}
}
},
"submit": {
"production": {
"android": {
"serviceAccountKeyPath": "./google-play-key.json",
"track": "internal"
}
}
}
}

APK Build Command
# Development APK (for testing on device)
eas build --platform android --profile preview

# Production App Bundle (for Play Store)
eas build --platform android --profile production

# OTA update push (no store review needed for JS changes)
eas update --branch production --message 'Hotfix: auth token refresh'

7.2  Offline-First Architecture for Mobile
•	MMKV storage for React Query persistent cache (survives app restart)
•	Background sync: expo-background-fetch for grade/notification refresh
•	Optimistic UI updates: mutations update cache before server confirmation
•	Conflict resolution: server-wins strategy on sync (server timestamp authoritative)
•	Exam sessions: fully offline-capable with encrypted local submission queue


8. QUICK REFERENCE CARDS

8.1  Service Port Map
Service
HTTP
gRPC
Debug
DB Schema
api-gateway
8080
—
5005
—
auth-svc
8081
9091
5006
auth_schema
parent-svc
8082
9092
5007
parent_schema
center-svc
8083
9093
5008
center_schema
assess-svc
8084
9094
5009
assess_schema
psych-svc
8085
9095
5010
psych_schema
ai-gateway
8086
—
5011
—
React Web
3000
—
—
—
Python AI
8095
—
—
psych_schema

8.2  Key Security Decisions (ADR Summary)
•	ADR-001: Argon2id over bcrypt — memory-hard, GPU-resistant, OWASP 2024 recommended
•	ADR-002: RS256 JWT over HS256 — asymmetric; services validate without knowing sign secret
•	ADR-003: Redis refresh token store — enables instant revocation (logout, compromise)
•	ADR-004: Device-bound refresh tokens — fingerprint = user-agent + IP subnet + device-id hash
•	ADR-005: mTLS between services — Istio STRICT; no plain HTTP in cluster
•	ADR-006: RBAC + ABAC hybrid — role for coarse access, attribute for fine-grained time/scope
•	ADR-007: Immutable audit log — Kafka topic retention = forever; no consumer-group deletion
•	ADR-008: Vault dynamic secrets — DB password rotates every 15 min; no static creds in env

8.3  Kafka Topic Registry
Topic
Partitions
Producer
Consumer(s)
edutech.auth.events
12
auth-svc
audit-svc, notify-svc
edutech.exam.submitted
24
assess-svc
grade-worker, psych-svc
edutech.grade.published
12
assess-svc
parent-svc, center-svc
edutech.ai.grade.request
12
assess-svc
ai-gateway-svc
edutech.ai.grade.response
12
ai-gateway
assess-svc
edutech.psych.completed
6
psych-svc
parent-svc, center-svc
edutech.notification.send
24
all svcs
notify-worker
edutech.audit.immutable
12
all svcs
audit-svc (read-only)



Document Version 1.0  |  EduTech AI Platform  |  Ready for Claude Code Sonnet 4.6
Use Stage prompts in Section 4 + Master Prompt in Section 6 for every Claude Code session

