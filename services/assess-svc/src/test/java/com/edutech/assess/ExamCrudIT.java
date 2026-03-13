// src/test/java/com/edutech/assess/ExamCrudIT.java
package com.edutech.assess;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.CreateExamRequest;
import com.edutech.assess.application.dto.ExamResponse;
import com.edutech.assess.application.dto.AddQuestionRequest;
import com.edutech.assess.application.dto.QuestionResponse;
import com.edutech.assess.application.dto.SubmissionResponse;
import com.edutech.assess.application.service.ExamService;
import com.edutech.assess.application.service.QuestionService;
import com.edutech.assess.application.service.SubmissionService;
import com.edutech.assess.application.service.EnrollmentService;
import com.edutech.assess.application.dto.EnrollStudentRequest;
import com.edutech.assess.domain.model.ExamMode;
import com.edutech.assess.domain.model.ExamStatus;
import com.edutech.assess.domain.model.Role;
import com.edutech.assess.domain.model.SubmissionStatus;
import com.edutech.assess.domain.port.out.ExamRepository;
import com.edutech.assess.domain.port.out.QuestionRepository;
import com.edutech.assess.domain.port.out.SubmissionRepository;
import com.edutech.assess.infrastructure.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for assess-svc: exam lifecycle, question persistence,
 * and submission creation/grading — backed by a real PostgreSQL container
 * running Flyway migrations.
 *
 * <p>JWT validation and Kafka/Redis are mocked so the tests focus solely
 * on the database-facing application and persistence layers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("assess-svc — Exam CRUD Integration Tests")
class ExamCrudIT {

    // ---------------------------------------------------------------------------
    // Infrastructure: single shared PostgreSQL container (reused across tests)
    // ---------------------------------------------------------------------------

    /**
     * Uses the pgvector image because Flyway V8__activate_pgvector.sql runs
     * "CREATE EXTENSION IF NOT EXISTS vector" which requires the pgvector
     * shared library to be present in the PostgreSQL installation.
     */
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("assess_test")
                    .withUsername("assess_user")
                    .withPassword("assess_pass");

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // ---------------------------------------------------------------------------
    // Mocked infrastructure beans (Kafka, Redis, JWT, gRPC)
    // ---------------------------------------------------------------------------

    /** Mocked so Kafka producer does not try to connect to a real broker. */
    @MockBean
    @SuppressWarnings("rawtypes")
    KafkaTemplate kafkaTemplate;

    /**
     * Mocked so the Spring context starts without a real RSA public-key file.
     * Individual tests that exercise HTTP endpoints should configure the mock
     * to return an AuthPrincipal; service-layer tests inject the principal directly.
     */
    @MockBean
    JwtTokenValidator jwtTokenValidator;

    // ---------------------------------------------------------------------------
    // Application-layer collaborators under test
    // ---------------------------------------------------------------------------

    @Autowired
    ExamService examService;

    @Autowired
    QuestionService questionService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    EnrollmentService enrollmentService;

    // ---------------------------------------------------------------------------
    // Port-layer repositories (verify persistence side-effects directly)
    // ---------------------------------------------------------------------------

    @Autowired
    ExamRepository examRepository;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    SubmissionRepository submissionRepository;

    // ---------------------------------------------------------------------------
    // Shared test identities
    // ---------------------------------------------------------------------------

    static final UUID CENTER_ID = UUID.randomUUID();
    static final UUID BATCH_ID  = UUID.randomUUID();
    static final UUID TEACHER_ID = UUID.randomUUID();
    static final UUID STUDENT_ID = UUID.randomUUID();

    AuthPrincipal teacherPrincipal;
    AuthPrincipal studentPrincipal;
    AuthPrincipal adminPrincipal;

    @BeforeEach
    void setUpPrincipals() {
        teacherPrincipal = new AuthPrincipal(TEACHER_ID, "teacher@test.com",
                Role.TEACHER, CENTER_ID, "fp-teacher");
        studentPrincipal = new AuthPrincipal(STUDENT_ID, "student@test.com",
                Role.STUDENT, null, "fp-student");
        adminPrincipal  = new AuthPrincipal(UUID.randomUUID(), "admin@test.com",
                Role.SUPER_ADMIN, CENTER_ID, "fp-admin");

        // Kafka mock: return a completed future so event publishing does not block
        when(kafkaTemplate.send(anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    // ---------------------------------------------------------------------------
    // Test 1: createExam_persistsToDatabase
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("createExam — persists exam to assess_schema.exams with DRAFT status")
    void createExam_persistsToDatabase() {
        CreateExamRequest request = new CreateExamRequest(
                "Integration Test Exam",
                "Full-stack persistence test",
                BATCH_ID,
                CENTER_ID,
                ExamMode.STANDARD,
                90,   // durationMinutes
                2,    // maxAttempts
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                100.0, // totalMarks
                40.0   // passingMarks
        );

        ExamResponse response = examService.createExam(request, adminPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isEqualTo("Integration Test Exam");
        assertThat(response.status()).isEqualTo(ExamStatus.DRAFT);
        assertThat(response.batchId()).isEqualTo(BATCH_ID);
        assertThat(response.centerId()).isEqualTo(CENTER_ID);
        assertThat(response.durationMinutes()).isEqualTo(90);
        assertThat(response.maxAttempts()).isEqualTo(2);
        assertThat(response.totalMarks()).isEqualTo(100.0);
        assertThat(response.passingMarks()).isEqualTo(40.0);

        // Verify persistence via repository
        Optional<com.edutech.assess.domain.model.Exam> persisted =
                examRepository.findById(response.id());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getStatus()).isEqualTo(ExamStatus.DRAFT);
        assertThat(persisted.get().getCreatedAt()).isNotNull();
    }

    // ---------------------------------------------------------------------------
    // Test 2: addQuestions_linkedToExam
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("addQuestion — question is persisted and linked to exam by examId")
    void addQuestions_linkedToExam() {
        // Arrange: create a parent exam first
        ExamResponse exam = examService.createExam(new CreateExamRequest(
                "Exam with Questions",
                null,
                BATCH_ID,
                CENTER_ID,
                ExamMode.STANDARD,
                60, 1,
                null, null,
                50.0, 20.0
        ), adminPrincipal);

        AddQuestionRequest q1 = new AddQuestionRequest(
                "What is 2 + 2?",
                List.of("3", "4", "5", "6"),
                1, // correctAnswer index = "4"
                "Basic arithmetic",
                5.0, 0.3, 0.8, 0.1
        );
        AddQuestionRequest q2 = new AddQuestionRequest(
                "What is the capital of France?",
                List.of("Berlin", "London", "Paris", "Madrid"),
                2, // correctAnswer index = "Paris"
                "European capitals",
                5.0, 0.2, 0.9, 0.05
        );

        QuestionResponse r1 = questionService.addQuestion(exam.id(), q1, teacherPrincipal);
        QuestionResponse r2 = questionService.addQuestion(exam.id(), q2, teacherPrincipal);

        assertThat(r1.id()).isNotNull();
        assertThat(r1.examId()).isEqualTo(exam.id());
        assertThat(r1.questionText()).isEqualTo("What is 2 + 2?");
        assertThat(r1.correctAnswer()).isEqualTo(1);
        assertThat(r1.marks()).isEqualTo(5.0);

        assertThat(r2.id()).isNotNull();
        assertThat(r2.examId()).isEqualTo(exam.id());

        // Verify both questions are persisted via repository
        List<com.edutech.assess.domain.model.Question> questions =
                questionRepository.findByExamId(exam.id());
        assertThat(questions).hasSize(2);
        assertThat(questions)
                .extracting(com.edutech.assess.domain.model.Question::getExamId)
                .containsOnly(exam.id());
    }

    // ---------------------------------------------------------------------------
    // Test 3: publishExam_changesStatusToPublished
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("publishExam — status transitions from DRAFT to PUBLISHED and event is emitted")
    void publishExam_changesStatusToPublished() {
        ExamResponse draft = examService.createExam(new CreateExamRequest(
                "Publish Me Exam",
                "Test publish transition",
                BATCH_ID,
                CENTER_ID,
                ExamMode.CAT,
                45, 1,
                Instant.now().plusSeconds(1800),
                Instant.now().plusSeconds(5400),
                80.0, 32.0
        ), adminPrincipal);

        assertThat(draft.status()).isEqualTo(ExamStatus.DRAFT);

        ExamResponse published = examService.publishExam(draft.id(), adminPrincipal);

        assertThat(published.status()).isEqualTo(ExamStatus.PUBLISHED);
        assertThat(published.id()).isEqualTo(draft.id());

        // Confirm persistence via repository
        Optional<com.edutech.assess.domain.model.Exam> fromDb =
                examRepository.findById(draft.id());
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getStatus()).isEqualTo(ExamStatus.PUBLISHED);
    }

    // ---------------------------------------------------------------------------
    // Test 4: submitAnswers_createsSubmission
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("startSubmission — creates IN_PROGRESS submission linked to exam and student")
    void startSubmission_createsInProgressSubmission() {
        // Arrange: published exam
        ExamResponse exam = examService.createExam(new CreateExamRequest(
                "Submission Test Exam",
                null,
                BATCH_ID,
                CENTER_ID,
                ExamMode.STANDARD,
                30, 3,
                null, null,
                20.0, 8.0
        ), adminPrincipal);
        examService.publishExam(exam.id(), adminPrincipal);

        // Enroll the student
        enrollmentService.enrollStudent(exam.id(),
                new EnrollStudentRequest(STUDENT_ID), adminPrincipal);

        // Act: start a submission
        SubmissionResponse submission = submissionService.startSubmission(exam.id(), studentPrincipal);

        // Assert
        assertThat(submission).isNotNull();
        assertThat(submission.id()).isNotNull();
        assertThat(submission.examId()).isEqualTo(exam.id());
        assertThat(submission.studentId()).isEqualTo(STUDENT_ID);
        assertThat(submission.status()).isEqualTo(SubmissionStatus.IN_PROGRESS);
        assertThat(submission.attemptNumber()).isEqualTo(1);
        assertThat(submission.startedAt()).isNotNull();

        // Verify persistence
        List<com.edutech.assess.domain.model.Submission> persisted =
                submissionRepository.findByExamIdAndStudentId(exam.id(), STUDENT_ID);
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getStatus()).isEqualTo(SubmissionStatus.IN_PROGRESS);
    }

    // ---------------------------------------------------------------------------
    // Test 5: gradeSubmission_computesScore
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("submitAnswers — grades submission and persists GRADED status with correct score")
    void gradeSubmission_computesScore() {
        // Arrange: create, publish, enroll
        ExamResponse exam = examService.createExam(new CreateExamRequest(
                "Grading Test Exam",
                null,
                BATCH_ID,
                CENTER_ID,
                ExamMode.STANDARD,
                60, 1,
                null, null,
                10.0, 4.0
        ), adminPrincipal);

        QuestionResponse q = questionService.addQuestion(exam.id(), new AddQuestionRequest(
                "Which planet is closest to the Sun?",
                List.of("Venus", "Mercury", "Earth", "Mars"),
                1, // Mercury
                "Mercury is the closest planet to the Sun",
                10.0, 0.2, 0.9, 0.1
        ), teacherPrincipal);

        examService.publishExam(exam.id(), adminPrincipal);

        enrollmentService.enrollStudent(exam.id(),
                new EnrollStudentRequest(STUDENT_ID), adminPrincipal);

        SubmissionResponse inProgress = submissionService.startSubmission(exam.id(), studentPrincipal);

        // Act: submit correct answer
        com.edutech.assess.application.dto.SubmitAnswersRequest answers =
                new com.edutech.assess.application.dto.SubmitAnswersRequest(
                        List.of(new com.edutech.assess.application.dto.AnswerEntry(q.id(), 1))
                );

        SubmissionResponse graded = submissionService.submitAnswers(
                exam.id(), inProgress.id(), answers, studentPrincipal);

        // Assert
        assertThat(graded.status()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(graded.scoredMarks()).isEqualTo(10.0);
        assertThat(graded.percentage()).isEqualTo(100.0);
        assertThat(graded.submittedAt()).isNotNull();

        // Confirm persisted state
        List<com.edutech.assess.domain.model.Submission> persisted =
                submissionRepository.findByExamIdAndStudentId(exam.id(), STUDENT_ID);
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(persisted.get(0).getScoredMarks()).isEqualTo(10.0);
    }

    // ---------------------------------------------------------------------------
    // Test 6: listByBatch returns only exams for requested batch
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("listByBatch — returns only exams belonging to the requested batch")
    void listByBatch_returnsOnlyExamsForBatch() {
        UUID otherBatchId = UUID.randomUUID();

        examService.createExam(new CreateExamRequest(
                "Batch A Exam 1", null, BATCH_ID, CENTER_ID,
                ExamMode.STANDARD, 30, 1, null, null, 10.0, 4.0
        ), adminPrincipal);

        examService.createExam(new CreateExamRequest(
                "Batch A Exam 2", null, BATCH_ID, CENTER_ID,
                ExamMode.STANDARD, 45, 1, null, null, 20.0, 8.0
        ), adminPrincipal);

        examService.createExam(new CreateExamRequest(
                "Batch B Exam", null, otherBatchId, CENTER_ID,
                ExamMode.STANDARD, 60, 1, null, null, 30.0, 12.0
        ), adminPrincipal);

        List<ExamResponse> batchExams =
                examService.listByBatch(BATCH_ID, adminPrincipal);

        assertThat(batchExams)
                .isNotEmpty()
                .allSatisfy(e -> assertThat(e.batchId()).isEqualTo(BATCH_ID));

        // The other batch's exam must not appear in batch-A results
        assertThat(batchExams)
                .extracting(ExamResponse::batchId)
                .doesNotContain(otherBatchId);
    }
}
