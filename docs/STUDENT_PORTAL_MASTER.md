# EduPath — Student Career Intelligence Portal
## Master Design Document · Research-Backed · World-Class AI Features

> **Target**: Students Class 10 and above · Competitive Exam Aspirants · Career Seekers
> **Vision**: The most intelligent, personalized, and holistic student career platform ever built — combining psychometric depth, adaptive AI learning, and real-time career intelligence into one unified experience.

---

## 1. Platform Overview

EduPath is a dedicated student career intelligence portal built on top of the EduTech AI Platform, designed specifically for students from Class 10 onward. Unlike generic EdTech platforms, EduPath provides a **holistic, longitudinal view** of a student's journey — from board exams through competitive entrance tests, stream selection, career path prediction, and college admission — powered by 20 world-class AI features backed by the latest research.

### Core Problems Solved

| Problem | EduPath Solution |
|---|---|
| "Which career is right for me?" | Career Oracle Engine — ML + psychometric fusion, 88-92% accuracy |
| "Where am I weak?" | Neural Weak Area Radar — 100,000+ concept knowledge graph, Bayesian KT |
| "How do I prepare for JEE/NEET?" | Adaptive Cognitive Engine — IRT 3PLM, personalized difficulty calibration |
| "Will I make it?" | ExamReadiness Score — composite ML score updated after every session |
| "I'm stuck on a doubt at midnight" | 24/7 AI Doubt Resolver — multi-modal RAG pipeline |
| "I'm losing motivation" | Dropout Predictor + Smart Intervention Engine — 30-day early warning |
| "My parents want updates" | Parent Transparency AI — auto-generated weekly natural language reports |
| "Which college should I target?" | College Cutoff Predictor — regression on 5 years of admission data |

### Supported Competitive Exams

JEE Main · JEE Advanced · NEET-UG · NEET-PG · UPSC CSE · GATE · CAT · MAT · XAT
CLAT · NDA · CDS · SSC CGL · IBPS PO · SBI PO · CUET · BITSAT · VITEEE · State PSCs

---

## 2. World-Class AI Feature Set (20 Features)

### TIER 1 — Foundational Intelligence (Core MVP)

#### AI-01 · Adaptive Cognitive Engine (ACE)
**Research basis**: Item Response Theory 3-Parameter Logistic Model (3PLM) + Bayesian Knowledge Tracing
**How it works**:
- Every question has calibrated parameters: difficulty (b), discrimination (a), guessing (c)
- Student's latent ability θ (theta) is continuously estimated via Expectation-Maximization
- Next question selected to maximize information at current θ level (Fisher Information)
- Multi-dimensional IRT (MIRT) tracks ability across sub-domains simultaneously
- Confidence intervals on θ narrow with each response — more accurate than simple scoring

**Differentiation from Embibe/competitors**: We use full 3PLM (not simpler 1PL Rasch model), MIRT for cross-subject learning, and integrate forgetting curves into ability estimation.

**Outcome**: Students always work at the exact right difficulty level — not too easy (boring), not too hard (discouraging). 40% faster concept mastery demonstrated in research.

---

#### AI-02 · Neural Weak Area Radar (NWAR)
**Research basis**: Knowledge graph + Bayesian Knowledge Tracing (BKT) + NLP concept tagging
**How it works**:
- 100,000+ concepts tagged across all subjects and exams (JEE/NEET/UPSC/etc.)
- Directed graph: concepts have prerequisite dependencies (e.g., Thermodynamics → Kinetic Theory → Ideal Gas Law)
- BKT estimates P(mastered) per concept after each interaction
- Root-cause classifier distinguishes: conceptual gap / calculation error / time pressure / topic unfamiliarity
- Prerequisite chain analysis: if student struggles with Integration, NWAR identifies whether Differentiation prerequisites are mastered

**Output**: Priority heat map of topics with estimated impact on exam score if addressed.

---

#### AI-03 · ExamReadiness Score (ERS)
**Research basis**: Composite ML ensemble, calibrated against historical JEE/NEET results
**Formula**:
```
ERS = w1·SyllabusCoverage + w2·MockTrendScore + w3·MasteryAvg
    + w4·TimeManagementScore + w5·AccuracyConsistency
```
- Weights learned from historical data (students who scored X% had ERS Y at T-30 days)
- Recalculated after every practice session (real-time)
- Maps to: predicted percentile, predicted rank, college eligibility list
- Trajectory analysis: "At this rate, your ERS reaches 85 in 47 days"

---

#### AI-04 · Smart Study Planner (SSP)
**Research basis**: Constraint satisfaction + LLM + SM-2 spaced repetition algorithm
**How it works**:
- Input: exam date, daily available hours, weak areas (from NWAR), current ERS
- LLM generates 90/180/365-day strategic plan
- Daily plan built using SM-2 algorithm: schedules each concept review just before the forgetting curve hits
- Fatigue model: reduces difficulty after 90+ minutes of continuous studying
- Dynamic re-planning: adjusts automatically when mock test scores change significantly
- Integration with calendar (Google/Apple) via iCal export

**Output**: Day-by-day study schedule with topic, duration, and resources recommended.

---

#### AI-05 · 24/7 AI Doubt Resolver (AIDR)
**Research basis**: RAG pipeline (Retrieval-Augmented Generation) + multi-modal LLM
**How it works**:
- Multi-modal input: text + image upload (handwritten question photo) + voice
- Vector similarity search over curated knowledge base (pgvector) to retrieve relevant concepts
- LLM generates solution using Socratic method: guides student to answer, doesn't just give it
- Step-by-step breakdown with explanation of each step
- Similar problem recommendations (cosine similarity over question embeddings)
- Recurring doubt tracking: identifies if same student keeps doubting the same concept → escalates to human mentor

**Differentiation**: Uses Socratic method (like Khanmigo) — doesn't just give answers, builds understanding.

---

### TIER 2 — Predictive Intelligence

#### AI-06 · Dropout Risk Predictor (DRP)
**Research basis**: LSTM networks on LMS behavioral sequences — AUC 0.80+ by Week 3 (Nature Scientific Reports 2025)
**Input features (time-series)**:
- Login frequency and session duration
- Question attempt rate
- Score trend (moving average)
- Time-on-task per subject
- Forum/chat engagement
- Study plan adherence rate
- Response latency changes

**Model**: LSTM (bidirectional) + attention mechanism over weekly behavioral sequences
**Output**: Risk score (0-1), risk level (GREEN/AMBER/RED), primary contributing factor
**Action cascade**:
- GREEN: No action
- AMBER: AI sends motivational nudge + adjusts study plan to easier wins
- RED: Triggers mentor intervention + optional parent alert (with consent)

---

#### AI-07 · Career Oracle Engine (COE)
**Research basis**: ML ensemble (Random Forest + Neural Network) — 88-92% accuracy (IARJSET 2025)
**Input features**:
- Academic performance (subject-wise, trend, board results)
- Psychometric profile (Big Five OCEAN + Holland RIASEC from psych-svc)
- Stated interests + activity patterns
- Family background (optional)
- Geographic preferences
- Current market demand signals (job postings API)

**Output**:
- Top 5 career paths with probability scores
- 10-year salary trajectory per path (percentile: P25/P50/P75)
- Required skill gaps
- Suggested exam pathways (e.g., for Software Engineering → JEE → B.Tech → GATE)
- Time-to-career estimate

---

#### AI-08 · College Cutoff Predictor (CCP)
**Research basis**: Regression models trained on 5 years of JEE/NEET/CUET admission data
**How it works**:
- Historical rank-to-college admission data for 5,000+ colleges
- Regression model: input current ERS → predicted future rank → eligible colleges
- Trajectory simulation: "If ERS grows 2 points/week, you qualify for IIT Bombay CSE by June"
- Scholarship eligibility matching (merit-based cutoffs)
- Dynamic shortlist updates as ERS changes

---

#### AI-09 · Performance DNA Analysis
**Research basis**: Longitudinal learning analytics, education data mining
**Dimensions analyzed**:
- Multi-year academic trajectory (slope, acceleration, inflection points)
- Circadian performance patterns (peak hour detection: morning/evening learner)
- Error pattern taxonomy: silly mistakes / conceptual gaps / time pressure / topic gaps
- Subject velocity (rate of improvement: fast-learner subjects vs. plateaued subjects)
- Exam-day vs. practice performance delta (pressure response analysis)
- Focus score trend (session duration × accuracy correlation)

---

#### AI-10 · Peer Intelligence Benchmarking (PIB)
**Research basis**: Collaborative filtering + cohort analysis, privacy-preserving
**Cohort matching criteria**: target exam + board type + current grade + geography
**Output insights**:
- "You are in the 67th percentile among 12,430 JEE aspirants from Tamil Nadu"
- "Top performers in your cohort study an average of 6.2 hours/day — you study 4.1"
- "Students who improved from ERS 62 to 80+ within 45 days: 73% increased mock test frequency to 3+/week"
- "Your Physics mastery (P68) is stronger than 68% of your cohort — leverage this"

**Privacy**: All benchmarking is anonymous — no student identity revealed.

---

### TIER 3 — Personalization Intelligence

#### AI-11 · Smart Content Recommender (SCR)
**Research basis**: Hybrid collaborative filtering (CF) + content-based filtering
**Signal inputs**:
- Student's current θ (IRT ability estimate)
- Learning style (VARK model)
- Time available for session
- Weak areas (NWAR output)
- Content engagement history (completion rate, re-watch rate, ratings)

**CF component**: "Students with similar profiles who improved ERS by 10+ points in 30 days consumed this content"
**Output**: Curated daily feed of video lectures, practice sets, articles, and formula sheets — difficulty-calibrated.

---

#### AI-12 · Spaced Repetition & Active Recall Engine (SRARE)
**Research basis**: SM-2 algorithm (Ebbinghaus Forgetting Curve) — 40-60% retention improvement
**How it works**:
- Leitner box system: concepts promoted/demoted based on recall accuracy
- SM-2 optimal inter-repetition intervals: next review scheduled at the moment of near-forgetting
- Auto-generates flashcards from study notes and video transcripts (NLP extraction)
- Active recall quizzes (fill-in-the-blank, definition → concept, formula recall)
- Session begins with spaced review before introducing new material

---

#### AI-13 · Learning Style Detector
**Research basis**: VARK model (Visual, Auditory, Reading/Writing, Kinesthetic)
**Detection method**: Passive inference from interaction patterns:
- High video engagement → Auditory/Visual
- Long time on text articles, note-taking → Reading/Writing
- High practice problem engagement → Kinesthetic
- Diagram/image dwell time → Visual

**Output**: VARK profile with confidence scores. Adapts content recommendations to detected style.

---

#### AI-14 · Smart Mock Test Intelligence (SMTI)
**Features**:
- **Adaptive mock generation**: Questions selected using ACE (AI-01) for personalized difficulty
- **Time management coach**: Flags questions spent too long on; trains optimal time allocation
- **Post-test deep analysis**: Topic-wise accuracy, attempt order analysis, time distribution heatmap
- **Question difficulty calibration**: Identifies questions that "look hard but were solved" vs. "should have been easy"
- **Exam-simulation mode**: Real exam conditions (timed, no hints, proctored)
- **NTA/NEET/UPSC pattern analysis**: Mock paper generated to match latest exam pattern

---

#### AI-15 · Multi-language AI Tutor
**Languages**: English + Hindi + Tamil + Telugu + Kannada + Malayalam + Bengali + Marathi + Gujarati
**Detection**: Identifies student's preferred language from first 3 interactions
**Implementation**:
- LLM prompted with language instruction
- Transliteration support (Roman script for Indian languages)
- Regional curriculum adaptation (CBSE vs. State Board syllabus differences)
- Voice input/output in regional languages

---

### TIER 4 — Mentoring & Wellness Intelligence

#### AI-16 · Mental Health Companion (MHC)
**Research basis**: Behavioral indicator analysis + validated screening tools
**Signals monitored**:
- Late-night study session frequency (>11 PM)
- Performance decline despite increased effort
- Reduced engagement / session abandonment patterns
- Message sentiment analysis (NLP on chat/doubt submissions)
- Sleep pattern proxy (study timing patterns)

**Output**:
- Burnout risk score (LOW/MEDIUM/HIGH)
- Suggested micro-breaks, mindfulness exercises
- Motivational content curation
- Parent/counselor alert (with explicit student consent)
- PHQ-4 adapted well-being check (weekly 4-question prompt)

---

#### AI-17 · Parent Transparency Intelligence (PTI)
**How it works**:
- LLM generates weekly natural-language progress report for parents
- Non-technical language: "Arjun improved his Physics score by 15% this week, focusing on Optics"
- Exam countdown with readiness indicators: "67 days until JEE Main · ERS: 71/100 · Trajectory: On track"
- Alert generation: "We recommend a mentor session this week — Arjun's Chemistry accuracy has declined 3 consecutive days"
- Goal milestone notifications: "Priya has completed 80% of JEE Math syllabus 🎉"

---

#### AI-18 · Smart Intervention Engine (SIE)
**Cascading intervention model**:
```
Level 1 (AMBER risk): AI motivation nudge + study plan reset + easier win tasks
Level 2 (RED risk):   Peer study group match + AI mentor session (24/7)
Level 3 (72hr RED):   Human mentor intervention request (mentor-svc)
Level 4 (7-day RED):  Parent alert + counselor referral
```
**Effectiveness tracking**: Each intervention's impact on ERS and engagement measured over 14 days.

---

#### AI-19 · Competitive Intelligence Feed (CIF)
**How it works**:
- Post-exam paper analysis (after JEE/NEET/UPSC releases)
- NLP extraction of topics, difficulty, question types from official papers
- YoY trend detection: "Modern Physics weight increased from 12% (2023) to 18% (2025)"
- Syllabus priority rebalancing: adjusts study plan weights based on trend
- "Hot topics" alerts: topics likely to appear based on trend analysis
- Peer performance on specific topics after mock papers

---

#### AI-20 · Voice AI Tutor (VAT)
**How it works**:
- Full conversational study sessions via voice (mobile/web)
- STT (Speech-to-Text) → Intent extraction → Doubt resolver (AI-05) → TTS (Text-to-Speech)
- Context window maintained across 30-minute sessions
- Socratic dialogue: asks follow-up questions to deepen understanding
- Subject-specific voice models (different tone for Math vs. Biology explanation)
- Session summary generated at end: "Today we covered Newton's 3rd Law, Circular Motion, and Projectile Motion"

---

## 3. Service Architecture (7 Services)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    EduPath Student Portal (Mobile + Web)                │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │ HTTPS
┌─────────────────────────────▼───────────────────────────────────────────┐
│                      student-gateway  :8093                             │
│           JWT GlobalFilter · Rate Limiting · Request-ID                 │
│                  Spring Cloud Gateway (WebFlux)                         │
└──┬──────────┬──────────┬──────────┬──────────┬──────────────────────────┘
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
[student  [exam-     [perform-  [ai-        [career-    [mentor-
profile]  tracker]  ance]      mentor]     oracle]     svc]
:8087     :8088      :8089      :8090       :8091       :8092
   │          │          │          │          │          │
   └──────────┴──────────┴──────────┴──────────┴──────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
         PostgreSQL        Redis           Kafka
         (6 schemas)    (ERS cache,     (7 topics)
                        session state)
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ai-gateway-svc   psych-svc     career-ai-svc
        (LLM routing)   (psychometric)  (Python ML)
```

### Service Inventory

| Service | Port | Package | DB Schema | Key Aggregates |
|---|---|---|---|---|
| student-profile-svc | 8087 | com.edutech.student | student_schema | StudentProfile, AcademicRecord, SubjectScore, TargetExam, StreamSelection |
| exam-tracker-svc | 8088 | com.edutech.examtracker | examtracker_schema | CompetitiveExam, ExamEnrollment, SyllabusModule, MockTestAttempt, StudySession |
| performance-svc | 8089 | com.edutech.performance | performance_schema | PerformanceSnapshot, WeakAreaRecord, ReadinessScore, SubjectMastery |
| ai-mentor-svc | 8090 | com.edutech.aimentor | None (reactive, stateless) | StudyPlan, DoubtSession (in-memory) |
| career-oracle-svc | 8091 | com.edutech.career | career_schema | CareerAssessment, CareerPath, CollegeRecommendation, SkillGap |
| mentor-svc | 8092 | com.edutech.mentor | mentor_schema | MentorProfile, MentorSession, InterventionAlert, MentoringGoal |
| student-gateway | 8093 | com.edutech.studentgateway | None (gateway) | Routes, Filters |

### Kafka Topics (Student Portal)

| Topic Env Var | Purpose | Producers | Consumers |
|---|---|---|---|
| KAFKA_TOPIC_STUDENT_EVENTS | Profile & academic updates | student-profile-svc | performance-svc, career-oracle-svc |
| KAFKA_TOPIC_EXAM_EVENTS | Mock tests, study sessions | exam-tracker-svc | performance-svc |
| KAFKA_TOPIC_PERFORMANCE_EVENTS | ERS updates, weak areas | performance-svc | mentor-svc, ai-mentor-svc |
| KAFKA_TOPIC_CAREER_EVENTS | Career recommendations | career-oracle-svc | mentor-svc |
| KAFKA_TOPIC_MENTOR_EVENTS | Session, interventions | mentor-svc | notification pipeline |
| KAFKA_TOPIC_AI_MENTOR_EVENTS | Doubts resolved, plans generated | ai-mentor-svc | analytics pipeline |
| KAFKA_TOPIC_AUDIT_IMMUTABLE | All audit events | All services | Compliance |

---

## 4. Data Model Highlights

### student_schema
```sql
students            — core profile, board, stream, city, target_year
academic_records    — per-year results, CGPA, board scores
subject_scores      — subject × year × score × board
target_exams        — student_id × exam_code × target_date × priority
stream_selections   — PCM / PCB / Commerce / Arts + motivation notes
```

### examtracker_schema
```sql
competitive_exams   — JEE/NEET/UPSC master registry with syllabus tree
exam_enrollments    — student × exam × target_year × status
syllabus_modules    — topic × subject × exam_code × difficulty_weight
mock_test_attempts  — attempt metadata, score, accuracy, time_taken
study_sessions      — date × subject × duration × questions_attempted × ERS_delta
```

### performance_schema (TimescaleDB — time-series)
```sql
performance_snapshots — student × timestamp × ERS × theta × percentile (hypertable)
weak_area_records     — student × concept × mastery_level × error_type × reviewed_at
subject_mastery       — student × subject × mastery_pct × velocity × last_updated
readiness_scores      — student × exam × score × components_json × projected_rank
```

### career_schema
```sql
career_assessments   — assessment metadata, psychometric_snapshot, academic_snapshot
career_paths         — recommended paths × probability × salary_p50 × timeline
college_recommendations — college × branch × predicted_cutoff × eligibility_pct
skill_gaps           — skill × current_level × required_level × suggested_resources
```

### mentor_schema
```sql
mentor_profiles      — mentor details, specializations, availability, rating
mentor_sessions      — booking, status, notes, outcome, ERS_before/after
intervention_alerts  — student × risk_level × trigger × cascade_level × resolved
mentoring_goals      — goal × target_ERS × deadline × milestones
```

---

## 5. Build Order

```
1. student-profile-svc  (foundation — all others need student identity)
2. exam-tracker-svc     (exam registry — feeds performance service)
3. performance-svc      (analytics — consumes student + exam events)
4. ai-mentor-svc        (reactive AI layer — uses performance data)
5. career-oracle-svc    (career prediction — uses student + psychometric)
6. mentor-svc           (human mentoring — consumes performance events)
7. student-gateway      (edge — routes to all above)
```

---

## 6. Tech Stack Additions

| Addition | Purpose |
|---|---|
| TimescaleDB (existing) | Time-series performance snapshots |
| pgvector (existing) | Doubt/question embeddings for RAG |
| career-ai-svc (Python FastAPI sidecar) | Career Oracle ML model serving |
| WebFlux (ai-mentor-svc) | Reactive streaming for AI responses |
| Resilience4j (existing) | Circuit breaker on AI provider calls |
| Redis (existing) | ERS caching, session state |
| Kafka (existing) | Event-driven performance updates |

---

## 7. Competitive Differentiation Matrix

| Feature | EduPath | Embibe | Khanmigo | Unacademy |
|---|---|---|---|---|
| IRT 3PLM Adaptive Testing | ✅ MIRT | ✅ BKT only | ❌ | ❌ |
| Career Oracle (ML + Psychometric) | ✅ Full | ❌ | ❌ | ❌ |
| Dropout Predictor (LSTM) | ✅ | ❌ | ❌ | ❌ |
| Multi-exam Support (20+ exams) | ✅ | Partial | ❌ | Partial |
| Human Mentor Integration | ✅ | ❌ | ❌ | ✅ |
| Parent Transparency AI | ✅ NL reports | ❌ | ❌ | Basic |
| Regional Language AI Tutor | ✅ 9 languages | ❌ | Limited | Hindi only |
| Mental Health Companion | ✅ | ❌ | ❌ | ❌ |
| College Cutoff Predictor | ✅ 5-yr data | Basic | ❌ | ❌ |
| Socratic Doubt Resolution | ✅ | ❌ | ✅ | ❌ |
| Spaced Repetition Engine | ✅ SM-2 | Partial | ❌ | ❌ |
| Voice AI Tutor | ✅ | ❌ | Partial | ❌ |
| Competitive Intelligence Feed | ✅ | ❌ | ❌ | ❌ |
| Performance DNA Analysis | ✅ | Basic | ❌ | ❌ |

---

*Generated: March 2026 · EduTech AI Platform · EduPath Student Career Intelligence Portal*
