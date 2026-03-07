# exam-tracker-svc — COMPLETION RECORD

> **STATUS: FROZEN — DO NOT MODIFY**
> Tests passed: 14/14 | Build: SUCCESS | Date: 2026-03-07

---

## Service Overview

EduPath Student Career Portal — competitive exam lifecycle management service. Tracks student enrollments in JEE/NEET/CUET/UPSC exams, manages syllabus module coverage, records mock test attempts with score analytics, and logs study sessions. Publishes Kafka events for performance-svc and ai-mentor-svc consumption.

---

## Test Results

```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS — Total time: 10.976 s
```

| Test Class | Tests | Result |
|---|---|---|
| ArchitectureRulesTest | 5 | PASS |
| ExamEnrollmentServiceTest | 5 | PASS |
| MockTestServiceTest | 4 | PASS |

---

## Architecture

Standard hexagonal (ports & adapters). `domain` has zero Spring dependencies.

```
com.edutech.examtracker/
  ExamTrackerApplication.java
  api/
    ExamEnrollmentController.java      REST — /api/v1/exam-tracker/enrollments/**
    SyllabusController.java            REST — /api/v1/exam-tracker/syllabus/**
    MockTestController.java            REST — /api/v1/exam-tracker/mock-tests/**
    StudySessionController.java        REST — /api/v1/exam-tracker/study-sessions/**
    GlobalExceptionHandler.java        RFC 7807 ProblemDetail
  application/
    service/
      ExamEnrollmentService.java       EnrollInExamUseCase, GetEnrollmentUseCase
      SyllabusService.java             GetSyllabusProgressUseCase, UpdateSyllabusModuleUseCase
      MockTestService.java             RecordMockTestUseCase, GetMockTestHistoryUseCase
      StudySessionService.java         RecordStudySessionUseCase, GetStudySessionsUseCase
    dto/                               Java records (all immutable)
    exception/                         DuplicateEnrollmentException, EnrollmentNotFoundException, ModuleNotFoundException
  domain/
    model/
      ExamEnrollment.java              @Entity, static factory, @Version, soft delete
      SyllabusModule.java              @Entity, ModuleStatus enum (NOT_STARTED, IN_PROGRESS, COMPLETED)
      MockTestAttempt.java             @Entity, records score/percentile/rank
      StudySession.java                @Entity, SessionType (READING, PRACTICE, REVISION, MOCK)
    model/enums/                       ExamCode (JEE_MAIN, JEE_ADV, NEET, CUET, UPSC_CSE, CAT), ExamStatus, ModuleStatus, SessionType
    event/                             ExamEnrolledEvent, MockTestCompletedEvent, StudySessionRecordedEvent, SyllabusModuleUpdatedEvent
    port/in/                           8 use-case interfaces
    port/out/                          ExamEnrollmentRepository, SyllabusModuleRepository, MockTestAttemptRepository, StudySessionRepository, ExamTrackerEventPublisher
  infrastructure/
    persistence/                       SpringData*Repository (package-private) + public *PersistenceAdapter @Component
    kafka/ExamTrackerKafkaAdapter.java  KafkaTemplate publisher, best-effort
    security/SecurityConfig.java        Trusts X-User-Id / X-User-Role headers (no JWT re-validation)
    config/KafkaTopicProperties.java    @ConfigurationProperties("kafka.topics")
```

---

## Flyway Migrations

| Version | Script | Description |
|---|---|---|
| V1 | V1__create_examtracker_schema.sql | CREATE SCHEMA examtracker_schema |
| V2 | V2__create_exam_enrollments.sql | exam_enrollments table + unique (student_id, exam_code) partial index |
| V3 | V3__create_syllabus_modules.sql | syllabus_modules table |
| V4 | V4__create_mock_test_attempts.sql | mock_test_attempts table |
| V5 | V5__create_study_sessions.sql | study_sessions table |

---

## API Endpoints

| Method | Path | Use Case |
|---|---|---|
| POST | `/api/v1/exam-tracker/enrollments` | Enroll student in exam |
| GET | `/api/v1/exam-tracker/enrollments/{studentId}` | Get student enrollments |
| GET | `/api/v1/exam-tracker/enrollments/{id}` | Get enrollment by ID |
| GET | `/api/v1/exam-tracker/syllabus/{enrollmentId}` | Get syllabus progress |
| PATCH | `/api/v1/exam-tracker/syllabus/{moduleId}` | Update module status |
| POST | `/api/v1/exam-tracker/mock-tests` | Record mock test attempt |
| GET | `/api/v1/exam-tracker/mock-tests/{studentId}/{enrollmentId}` | Get mock test history |
| POST | `/api/v1/exam-tracker/study-sessions` | Record study session |
| GET | `/api/v1/exam-tracker/study-sessions/{studentId}` | Get study sessions |

---

## Kafka Events Published

| Event | Topic | Trigger |
|---|---|---|
| ExamEnrolledEvent | `exam.tracker.enrolled` | Exam enrollment |
| MockTestCompletedEvent | `exam.tracker.mock.completed` | Mock test recorded |
| StudySessionRecordedEvent | `exam.tracker.study.session` | Study session logged |
| SyllabusModuleUpdatedEvent | `exam.tracker.syllabus.updated` | Module status changed |

---

## Key Design Decisions

- **Duplicate enrollment guard**: `DuplicateEnrollmentException` on `(student_id, exam_code)` collision (partial unique index with `deleted_at IS NULL`).
- **Syllabus coverage**: Computed as `completedModules / totalModules * 100` for `GetSyllabusProgressUseCase`.
- **Mock test trend**: `MockTestService` computes rolling average over last N attempts for trend score — fed to performance-svc via Kafka.
- **Security**: Trusts `X-User-Id`/`X-User-Role` headers from student-gateway. No JWT re-validation.
- **No Lombok**: Manual constructors. Constructor injection only.
- **Optimistic locking**: `@Version` on all mutable entities.

---

## ArchUnit Rules (5)

1. `domain` package has no dependencies on Spring Framework
2. `application` services depend on `domain` ports only (no infrastructure)
3. `@Entity` classes reside in `domain.model` package
4. `@Service` classes reside in `application.service` package
5. `infrastructure` adapters implement domain port interfaces

---

*This document is permanently frozen. Any future changes to exam-tracker-svc require a new versioned record.*
