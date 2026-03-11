# EduTech AI Platform — AI Features & Use Cases

> **Document status:** Living reference — 2026-03-10
> Covers all AI-powered features, the problems they solve, and the technical flow.

---

## Overview

All AI requests are routed through `ai-gateway-svc` (port 8086). Downstream services call
`POST /api/v1/ai/completions` with a service API key (`X-Service-Key`). The gateway handles
provider selection, rate limiting, and audit logging.

**Supported LLM Providers (LlmProvider enum):**

| Provider | Adapter | Use |
|---|---|---|
| `ANTHROPIC` | `AnthropicWebClientAdapter` (default/`@Primary`) | Parent copilot, AI Mentor doubts |
| `OPENROUTER` | `OpenRouterWebClientAdapter` | Multi-model fallback; OpenAI-compatible routing to `openai/gpt-4o-mini` |
| `OPENAI` | `OpenAiEmbeddingAdapter` | Vector embeddings only (EmbeddingController) |
| `OLLAMA` | `PsychAiSidecarWebClientAdapter` | Psychometric AI sidecar (career prediction) |

**Local-echo mode:** When any LLM API key starts with a placeholder prefix (`sk-ant-dev`, `or-dev`)
or is blank, the adapter's `isPlaceholderKey()` returns `true` and `executeLocalEcho()` is called
instead. All AI features work in development without real API keys.

---

## AI Use Cases

### 1. Parent Copilot — "How is my child doing?"

**Problem solved:** Parents struggle to interpret raw assessment scores and attendance data.
They need plain-language summaries and actionable advice without navigating multiple dashboards.

**User flow:**
1. Parent logs into the parent portal → Copilot tab
2. Selects a linked child (auto-selected if only one)
3. Sends a natural-language question (e.g. "Who is my child?" or "How is Arjun doing?")
4. `CopilotService` injects the student's real name and psychometric profile into the system prompt
5. Calls `POST /api/v1/ai/completions` on ai-gateway-svc
6. AI returns a personalised, factually grounded response (no hallucination)

**Technical flow:**
```
ParentCopilotController (parent-svc)
  → CopilotService.startConversation(parentId, studentId, message)
      → parentProfileRepository.findByUserId(userUuid)   ← resolves JWT userId → profile UUID
      → studentLinkRepository.findByParentIdAndStudentId(profileId, studentUuid)  ← gets studentName
      → psychSvcWebClient GET /api/v1/psych/profiles?studentId=...  ← fetches Big Five + RIASEC
      → aiGatewayWebClient POST /api/v1/ai/completions  ← system prompt includes studentName + psychContext
        → LlmRoutingService → OpenRouterWebClientAdapter (or local-echo)
```

**Critical architecture — student name resolution (FROZEN commit d0a1311):**
- JWT subject = user ID (`30a06234...`). `student_links.parent_id` stores **profile ID** (`59ca5bc7...`)
- Must resolve: `userUuid → parentProfileRepository → profileId → studentLinkRepository → studentName`
- Without this chain the system prompt has no name → LLM hallucinates ("John Doe, Delhi Public School")

**Frontend:** `src/pages/parent/ParentCopilotPage.tsx` — full-page Copilot at `/parent/copilot`
- `linkedStudents` query uses Page extraction: `const d = r.data; return Array.isArray(d) ? d : (d.content ?? [])`
- Without this, `studentId` is never populated → backend receives `studentId: ''` → no name injected

**Endpoints:**
- `POST /api/v1/copilot/conversations` — start conversation (with `studentId` in body)
- `POST /api/v1/copilot/conversations/{id}/messages` — continue conversation
- `GET /api/v1/copilot/conversations/{id}` — fetch conversation history

---

### 2. Student AI Mentor — Doubt Resolution

**Problem solved:** Students stuck on a concept or exam question need instant, personalised
help without waiting for a teacher. The AI mentor provides step-by-step guidance.

**User flow:**
1. Student opens AI Mentor tab → asks a doubt ("Why is the answer B and not C?")
2. ai-mentor-svc builds a contextual prompt with the student's learning history and the question context
3. Calls ai-gateway-svc completions endpoint
4. AI returns an explanation tailored to the student's level

**Technical flow:**
```
DoubtController (ai-mentor-svc)
  → POST /api/v1/ai/completions (X-Service-Key: edutech-internal-svc-2026)
    → LlmRoutingService → AnthropicWebClientAdapter
```

**Endpoints:**
- `POST /api/v1/doubts` — submit a doubt
- `GET /api/v1/doubts/{studentId}` — list resolved doubts
- `GET /api/v1/study-plans/{studentId}` — AI-generated study plan
- `GET /api/v1/recommendations/{studentId}` — AI-generated content recommendations

**Frontend:** `src/pages/student/` — AIMentorPage, StudyPlanPage

---

### 3. Career Oracle — AI Career Guidance

**Problem solved:** Students (especially Class 10–12) have no personalised career path
guidance. Raw aptitude scores don't translate to career recommendations on their own.

**User flow:**
1. Student completes psychometric assessment
2. Career oracle fetches RIASEC code + aptitude domain scores from psych-svc
3. Calls ai-gateway-svc for LLM-based career narrative
4. Returns: ranked career paths, college tier predictions, action plan

**Technical flow:**
```
CareerRecommendationService (career-oracle-svc)
  → GET /api/v1/psych/profiles/{profileId} (internal)
  → POST /api/v1/ai/completions → LlmRoutingService
  → Persists recommendations in career_oracle_db
```

**Endpoints:**
- `POST /api/v1/career-recommendations/students/{studentId}/generate` — trigger AI generation
- `GET /api/v1/career-recommendations/students/{studentId}` — fetch results
- `POST /api/v1/college-predictions/students/{studentId}/predict` — college match prediction
- `GET /api/v1/college-predictions/students/{studentId}` — fetch predictions
- `GET /api/v1/career-profiles/by-student/{studentId}` — aptitude profile

**Frontend:** `src/pages/career/CareerOraclePage.tsx` — all data live from career-oracle-svc

---

### 4. Psychometric Profiling — AI Personality & Aptitude Analysis

**Problem solved:** Traditional psychometric tests give raw scores. Students need
interpretation: what does a Big Five profile mean for their ideal learning style and career fit?

**User flow:**
1. Student takes the in-app psychometric assessment (multiple question sets)
2. psych-svc scores the responses (Big Five, RIASEC, domain aptitudes)
3. Calls Ollama sidecar (PsychAiSidecarWebClientAdapter) for ML-based personality narrative
4. Student sees their profile with interpreted traits and learning style recommendations

**Technical flow:**
```
PsychSessionController (psych-svc)
  → ScoreCalculationService (domain)
  → POST /api/v1/ai/career-predictions → CareerPredictionRoutingService
    → PsychAiSidecarWebClientAdapter → Python FastAPI psych-ai-svc
```

**Endpoints:**
- `GET /api/v1/psych/profiles?studentId={studentId}` — fetch profile (Page<> extraction)
- `GET /api/v1/psych/profiles/{profileId}/sessions` — session history
- `POST /api/v1/psych/profiles/{profileId}/sessions` — start new session

**Frontend:** `src/pages/psychometric/PsychometricPage.tsx` — all data live from psych-svc

---

### 5. OpenRouter Multi-Model Routing

**Problem solved:** Anthropic availability/cost. When the platform needs to route to
alternative models (GPT-4o-mini for lower cost, or any model on OpenRouter's catalog)
without changing application code.

**Configuration:**
```bash
OPENROUTER_API_KEY=<key from openrouter.ai/keys>
OPENROUTER_MODEL=arcee-ai/trinity-large-preview:free   # free tier; supports system prompts
```

**Active provider:** `AI_DEFAULT_PROVIDER=OPENROUTER` in `.env` → `LlmRoutingService.resolveProvider()`
routes all completions through OpenRouter by default. Switch to `ANTHROPIC` by changing this value.

OpenRouter supports: OpenAI GPT-4o, Anthropic Claude, Gemini, Mistral, Llama 3, and 100+ more.

---

## Admin — Batches & Assessments

### Batch Management

**Problem solved:** Admin needs to organise students into subject-based teaching groups,
track enrolment capacity, and monitor batch health before the academic cycle starts.

**Features (AdminBatchesPage.tsx — `/admin/batches`):**
- List all batches for the admin's center with enrolment stats
- Health summary: CRITICAL (<50% fill, red), WARNING (50–80%, yellow), HEALTHY (>80%, green)
- Create batch: name, code, subject, teacherId, max students, start/end dates
- All data live from center-svc `/api/v1/centers/{centerId}/batches`

### Assessment Management

**Problem solved:** Admin (or teacher acting as admin) needs to create, manage, and publish
online/offline/hybrid exams with MCQ question banks, then track student performance.

**Features (AdminAssessmentsPage.tsx — `/admin/assessments`):**
- List all exams with status, mode, duration, marks, question count
- Create exam: title, description, batch, mode, duration, total/passing marks, schedule
- Add MCQ questions: question text, 4 options, correct answer, marks per question
- Publish exam: changes status from DRAFT → PUBLISHED (makes it visible to students)
- All data live from assess-svc:
  - `GET/POST /api/v1/exams`
  - `PUT /api/v1/exams/{id}/publish`
  - `GET/POST /api/v1/exams/{id}/questions`

---

## Local Development Without API Keys

All AI features work in local development without real API keys:

| Env var | Placeholder value | Behaviour |
|---|---|---|
| `ANTHROPIC_API_KEY` | `sk-ant-dev-*` or blank | Local echo mode |
| `OPENROUTER_API_KEY` | `or-dev-placeholder` | Local echo mode |
| `OPENAI_API_KEY` | blank | Embedding calls return 503 gracefully |

Local echo mode inspects system-prompt/user-message keywords and returns contextually
appropriate canned responses — parent copilot, student doubts, career recommendations all
return sensible demo content without hitting any external API.

---

## AI Gateway Request Format

```json
POST /api/v1/ai/completions
X-Service-Key: edutech-internal-svc-2026
Authorization: Bearer <JWT>

{
  "requesterId": "parent-svc",
  "systemPrompt": "You are a helpful education advisor...",
  "userMessage": "How is my child doing in Math?",
  "maxTokens": 500,
  "temperature": 0.7
}
```

Response:
```json
{
  "content": "Based on recent assessments, Arjun scored...",
  "provider": "ANTHROPIC",
  "model": "claude-3-5-sonnet-20241022",
  "tokensUsed": 312
}
```
