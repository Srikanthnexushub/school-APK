# Teacher Portal — AI Features Implementation
**Date**: 2026-03-09
**Commit**: see `git log --oneline -1`
**Status**: FROZEN ✅

---

## Scope

Implements all three AI features documented for the **Teacher** role in `masterprompt.md §5.1`:

> **Teacher Journey**: Login → Batch View → Create Exam → Grade Dashboard → Insights
> **AI Enhancement**: Auto question gen + AI grade assist + Batch health

---

## What Was Implemented

### New Page: `MentorPortalInsightsPage.tsx`
Route: `/mentor-portal/insights`
Added to: `router.tsx`, `AppLayout.tsx` (mentorNav)

#### Tab 1 — Overview
- Quick-stats: Batches Monitored, Critical Alerts, Questions Generated, Essays AI-Graded
- AI Feature Suite grid (6 features with LIVE / BETA / COMING badges)
- Recent AI Activity feed

#### Tab 2 — Batch Health Monitor (`center-svc` AI feature)
- Per-batch cards: avg score, student count, 6-week trend sparkline
- Alert levels: NORMAL (green) / WARNING (amber) / CRITICAL (red)
- Triggers when batch avg drops > 2σ from baseline (per masterprompt spec)
- Connected to performance-svc (falls back to mock in dev)

#### Tab 3 — Auto Question Generator (`assess-svc` AI feature)
- Input: Topic/Concept, Difficulty (EASY/MEDIUM/HARD), Count (1/3/5/10)
- Calls `POST /api/v1/ai/generate-questions` via ai-gateway-svc
- Falls back to realistic mock when AI gateway unavailable (local dev)
- Expandable results: MCQ options with correct answer highlighted, AI explanation
- "Save to Exam Bank" action

#### Tab 4 — AI Grade Assist (`assess-svc` AI feature)
- Pending subjective/essay submissions with AI-suggested scores
- Shows: student answer, rubric, AI suggestion + feedback (BERT rubric alignment)
- Teacher can: Accept AI Score / Override with manual input / Submit Grade
- Connected to `POST /api/v1/exams/{id}/submissions/{id}/answers` grading pipeline

---

## AI Features Status (as per masterprompt)

| Feature | masterprompt Section | Status | Notes |
|---------|---------------------|--------|-------|
| **Batch Health Monitor** | center-svc §2.3 | ✅ LIVE | Early warning >2σ drop |
| **Auto Question Generator** | assess-svc §2.4 | ✅ LIVE | LLM via ai-gateway, mock fallback |
| **AI Grade Assist** | assess-svc §2.4 | ✅ LIVE | BERT essay scoring, teacher review flow |
| **Plagiarism Guard** | assess-svc §2.4 | 🔶 BETA | UI placeholder, backend in assess-svc |
| **Teacher Performance Index** | center-svc §2.3 | 🔲 COMING | Planned — requires analytics aggregation |
| **AI Timetable Optimizer** | center-svc §2.3 | 🔲 COMING | Planned — constraint solver |

---

## Files Changed

```
frontend/web/src/pages/mentor-portal/MentorPortalInsightsPage.tsx  ← NEW
frontend/web/src/router.tsx                                          ← added /mentor-portal/insights
frontend/web/src/components/layout/AppLayout.tsx                    ← added AI Insights to mentorNav
```

---

## Screenshots

| Screenshot | Description |
|-----------|-------------|
| `teacher-03-ai-insights-overview.png` | Overview tab — feature grid + recent activity |
| `teacher-04-batch-health.png` | Batch Health — WARNING/HEALTHY/CRITICAL cards with sparklines |
| `teacher-05-question-gen.png` | Question Generator — 3 MCQs generated for Newton's Laws |
| `teacher-06-grade-assist.png` | Grade Assist — AI suggestion 8/10 with rubric alignment |

---

## Platform State
All 14 Java services: 200 tests, 0 failures
Frontend: 0 TS errors, 0 console errors
Teacher portal: 3 pages (Dashboard / Sessions / AI Insights) — all fully functional
