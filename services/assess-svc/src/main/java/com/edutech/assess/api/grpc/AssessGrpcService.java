package com.edutech.assess.api.grpc;

import com.edutech.assess.application.dto.SimilarQuestionRequest;
import com.edutech.assess.application.dto.SimilarQuestionResponse;
import com.edutech.assess.application.exception.EnrollmentNotFoundException;
import com.edutech.assess.application.exception.QuestionNotFoundException;
import com.edutech.assess.application.service.QuestionEmbeddingService;
import com.edutech.assess.application.service.QuestionService;
import com.edutech.assess.domain.model.Question;
import com.edutech.assess.domain.port.out.ExamEnrollmentRepository;
import com.edutech.assess.domain.port.out.QuestionRepository;
import com.edutech.proto.assess.AssessServiceGrpc;
import com.edutech.proto.assess.EnrollmentResponse;
import com.edutech.proto.assess.GetEnrollmentRequest;
import com.edutech.proto.assess.GetQuestionRequest;
import com.edutech.proto.assess.ListQuestionsRequest;
import com.edutech.proto.assess.QuestionListResponse;
import com.edutech.proto.assess.QuestionResponse;
import com.edutech.proto.assess.SimilarQuestionsRequest;
import com.edutech.proto.assess.SubmitAnswerRequest;
import com.edutech.proto.assess.SubmitAnswerResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

/**
 * gRPC server implementation for the Assessment service.
 * Lives in the api layer (same as REST controllers). Delegates all business
 * logic to existing application services and domain repositories — no domain logic here.
 */
@GrpcService
public class AssessGrpcService extends AssessServiceGrpc.AssessServiceImplBase {

    private final QuestionRepository questionRepository;
    private final QuestionEmbeddingService questionEmbeddingService;
    private final ExamEnrollmentRepository enrollmentRepository;

    public AssessGrpcService(QuestionRepository questionRepository,
                              QuestionEmbeddingService questionEmbeddingService,
                              ExamEnrollmentRepository enrollmentRepository) {
        this.questionRepository = questionRepository;
        this.questionEmbeddingService = questionEmbeddingService;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public void getQuestion(GetQuestionRequest request,
                             StreamObserver<QuestionResponse> responseObserver) {
        try {
            UUID questionId = UUID.fromString(request.getQuestionId());
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new QuestionNotFoundException(questionId));

            responseObserver.onNext(toProtoQuestion(question));
            responseObserver.onCompleted();

        } catch (QuestionNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listQuestions(ListQuestionsRequest request,
                               StreamObserver<QuestionListResponse> responseObserver) {
        try {
            UUID examId = UUID.fromString(request.getExamId());
            List<Question> questions = questionRepository.findByExamId(examId);

            List<QuestionResponse> protoQuestions = questions.stream()
                    .map(this::toProtoQuestion)
                    .toList();

            QuestionListResponse response = QuestionListResponse.newBuilder()
                    .addAllQuestions(protoQuestions)
                    .setTotal(protoQuestions.size())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void findSimilarQuestions(SimilarQuestionsRequest request,
                                      StreamObserver<QuestionListResponse> responseObserver) {
        try {
            float[] embedding = toFloatArray(request.getQueryEmbeddingList());
            List<UUID> excludeIds = request.getExcludeIdsList().stream()
                    .map(UUID::fromString)
                    .toList();

            SimilarQuestionRequest similarRequest = new SimilarQuestionRequest(
                    embedding, request.getTopK(), excludeIds);
            List<SimilarQuestionResponse> similar =
                    questionEmbeddingService.findSimilarQuestions(similarRequest);

            List<QuestionResponse> protoQuestions = similar.stream()
                    .map(sr -> QuestionResponse.newBuilder()
                            .setId(sr.id().toString())
                            .setExamId(sr.examId().toString())
                            .setText(sr.questionText() != null ? sr.questionText() : "")
                            .addAllOptions(sr.options() != null ? sr.options() : List.of())
                            .setMarks(sr.marks())
                            .setDifficulty(sr.difficulty())
                            .setDiscriminationParam(sr.discrimination())
                            .setGuessingParam(sr.guessingParam())
                            .build())
                    .toList();

            QuestionListResponse response = QuestionListResponse.newBuilder()
                    .addAllQuestions(protoQuestions)
                    .setTotal(protoQuestions.size())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getExamEnrollment(GetEnrollmentRequest request,
                                   StreamObserver<EnrollmentResponse> responseObserver) {
        try {
            UUID enrollmentId = UUID.fromString(request.getEnrollmentId());
            var enrollment = enrollmentRepository.findById(enrollmentId)
                    .orElseThrow(() -> new EnrollmentNotFoundException(null, null));

            EnrollmentResponse response = EnrollmentResponse.newBuilder()
                    .setId(enrollment.getId().toString())
                    .setStudentId(enrollment.getStudentId().toString())
                    .setExamId(enrollment.getExamId().toString())
                    .setStatus(enrollment.getStatus().name())
                    .setEnrolledAt(enrollment.getEnrolledAt() != null
                            ? enrollment.getEnrolledAt().toString() : "")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (EnrollmentNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Enrollment not found: " + request.getEnrollmentId())
                    .asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void submitAnswer(SubmitAnswerRequest request,
                              StreamObserver<SubmitAnswerResponse> responseObserver) {
        try {
            UUID questionId = UUID.fromString(request.getQuestionId());
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new QuestionNotFoundException(questionId));

            boolean correct = request.getSelectedOption() == question.getCorrectAnswer();
            double marksAwarded = correct ? question.getMarks() : 0.0;
            String feedback = correct ? "Correct answer." : "Incorrect. Review this topic.";

            SubmitAnswerResponse response = SubmitAnswerResponse.newBuilder()
                    .setCorrect(correct)
                    .setMarksAwarded(marksAwarded)
                    .setFeedback(feedback)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (QuestionNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private QuestionResponse toProtoQuestion(Question q) {
        return QuestionResponse.newBuilder()
                .setId(q.getId().toString())
                .setExamId(q.getExamId().toString())
                .setText(q.getQuestionText() != null ? q.getQuestionText() : "")
                .setMarks(q.getMarks())
                .setDifficulty(q.getDifficulty())
                .setDiscriminationParam(q.getDiscrimination())
                .setGuessingParam(q.getGuessingParam())
                .build();
    }

    private float[] toFloatArray(List<Float> floatList) {
        float[] result = new float[floatList.size()];
        for (int i = 0; i < floatList.size(); i++) {
            result[i] = floatList.get(i);
        }
        return result;
    }
}
