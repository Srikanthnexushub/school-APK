package com.edutech.performance.api.grpc;

import com.edutech.performance.application.exception.ReadinessScoreNotFoundException;
import com.edutech.performance.application.service.ReadinessScoreService;
import com.edutech.performance.application.service.WeakAreaService;
import com.edutech.performance.domain.port.out.PerformanceSnapshotRepository;
import com.edutech.performance.domain.port.out.ReadinessTimeSeriesPort;
import com.edutech.performance.domain.service.DropoutRiskCalculator;
import com.edutech.proto.performance.DailyReadiness;
import com.edutech.proto.performance.DropoutRiskRequest;
import com.edutech.proto.performance.DropoutRiskResponse;
import com.edutech.proto.performance.GetReadinessRequest;
import com.edutech.proto.performance.PerformanceServiceGrpc;
import com.edutech.proto.performance.ReadinessTrendRequest;
import com.edutech.proto.performance.ReadinessTrendResponse;
import com.edutech.proto.performance.ReadinessResponse;
import com.edutech.proto.performance.WeakArea;
import com.edutech.proto.performance.WeakAreaListResponse;
import com.edutech.proto.performance.WeakAreaRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

/**
 * gRPC server implementation for the Performance service.
 * Lives in the api layer (same as REST controllers). Delegates all business
 * logic to existing application services — no domain logic here.
 */
@GrpcService
public class PerformanceGrpcServer extends PerformanceServiceGrpc.PerformanceServiceImplBase {

    private final ReadinessScoreService readinessScoreService;
    private final WeakAreaService weakAreaService;
    private final PerformanceSnapshotRepository snapshotRepository;
    private final ReadinessTimeSeriesPort readinessTimeSeriesPort;
    // Pure domain service — no Spring dependencies, safe to instantiate directly
    private final DropoutRiskCalculator dropoutRiskCalculator = new DropoutRiskCalculator();

    public PerformanceGrpcServer(ReadinessScoreService readinessScoreService,
                                   WeakAreaService weakAreaService,
                                   PerformanceSnapshotRepository snapshotRepository,
                                   ReadinessTimeSeriesPort readinessTimeSeriesPort) {
        this.readinessScoreService = readinessScoreService;
        this.weakAreaService = weakAreaService;
        this.snapshotRepository = snapshotRepository;
        this.readinessTimeSeriesPort = readinessTimeSeriesPort;
    }

    @Override
    public void getReadinessScore(GetReadinessRequest request,
                                   StreamObserver<ReadinessResponse> responseObserver) {
        try {
            UUID studentId = UUID.fromString(request.getStudentId());
            UUID enrollmentId = UUID.fromString(request.getEnrollmentId());

            var score = readinessScoreService.getLatestScore(studentId, enrollmentId);

            ReadinessResponse response = ReadinessResponse.newBuilder()
                    .setId(score.id().toString())
                    .setStudentId(score.studentId().toString())
                    .setEnrollmentId(score.enrollmentId().toString())
                    .setErsScore(score.ersScore().doubleValue())
                    .setSyllabusCoveragePercent(score.syllabusCoveragePercent().doubleValue())
                    .setMockTestTrendScore(score.mockTestTrendScore().doubleValue())
                    .setMasteryAverage(score.masteryAverage().doubleValue())
                    .setAccuracyConsistency(score.accuracyConsistency().doubleValue())
                    .setComputedAt(score.computedAt().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (ReadinessScoreNotFoundException e) {
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
    public void getDropoutRisk(DropoutRiskRequest request,
                                StreamObserver<DropoutRiskResponse> responseObserver) {
        try {
            UUID studentId = UUID.fromString(request.getStudentId());

            var latestSnapshot = snapshotRepository.findLatestByStudentId(studentId);
            if (latestSnapshot.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("No performance snapshots found for student: " + request.getStudentId())
                        .asRuntimeException());
                return;
            }

            // Use snapshots for risk calculation; pass null for lastLoginAt (not available via gRPC)
            List<com.edutech.performance.domain.model.PerformanceSnapshot> snapshots =
                    List.of(latestSnapshot.get());
            DropoutRiskCalculator.RiskAssessment assessment =
                    dropoutRiskCalculator.calculate(snapshots, null);

            DropoutRiskResponse response = DropoutRiskResponse.newBuilder()
                    .setStudentId(request.getStudentId())
                    .setRiskScore(assessment.riskScore().doubleValue())
                    .setRiskLevel(assessment.riskLevel().name())
                    .setPrimaryFactor(assessment.primaryFactor())
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
    public void getWeakAreas(WeakAreaRequest request,
                              StreamObserver<WeakAreaListResponse> responseObserver) {
        try {
            UUID studentId = UUID.fromString(request.getStudentId());
            UUID enrollmentId = UUID.fromString(request.getEnrollmentId());

            var weakAreas = weakAreaService.getWeakAreas(studentId, enrollmentId);

            List<WeakArea> protoWeakAreas = weakAreas.stream()
                    .map(wa -> WeakArea.newBuilder()
                            .setId(wa.id().toString())
                            .setStudentId(wa.studentId().toString())
                            .setSubject(wa.subject() != null ? wa.subject() : "")
                            .setTopicName(wa.topicName() != null ? wa.topicName() : "")
                            .setMasteryPercent(wa.masteryPercent().doubleValue())
                            .setPrimaryErrorType(wa.primaryErrorType() != null
                                    ? wa.primaryErrorType().name() : "")
                            .setDetectedAt(wa.detectedAt() != null ? wa.detectedAt().toString() : "")
                            .build())
                    .toList();

            WeakAreaListResponse response = WeakAreaListResponse.newBuilder()
                    .addAllWeakAreas(protoWeakAreas)
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
    public void getReadinessTrend(ReadinessTrendRequest request,
                                   StreamObserver<ReadinessTrendResponse> responseObserver) {
        try {
            int lastDays = request.getLastDays() > 0 ? request.getLastDays() : 30;
            List<ReadinessTimeSeriesPort.DailyReadiness> trend =
                    readinessTimeSeriesPort.getDailyReadiness(request.getStudentId(), lastDays);

            List<DailyReadiness> protoPoints = trend.stream()
                    .map(dr -> DailyReadiness.newBuilder()
                            .setDay(dr.day().toString())
                            .setAvgErsScore(dr.avgErsScore().doubleValue())
                            .setSnapshotCount(dr.snapshotCount())
                            .build())
                    .toList();

            ReadinessTrendResponse response = ReadinessTrendResponse.newBuilder()
                    .addAllPoints(protoPoints)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
