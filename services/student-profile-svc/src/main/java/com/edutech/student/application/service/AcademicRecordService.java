package com.edutech.student.application.service;

import com.edutech.student.application.dto.AcademicRecordResponse;
import com.edutech.student.application.dto.AddAcademicRecordRequest;
import com.edutech.student.application.exception.StudentNotFoundException;
import com.edutech.student.domain.event.AcademicRecordAddedEvent;
import com.edutech.student.domain.model.AcademicRecord;
import com.edutech.student.domain.port.in.AddAcademicRecordUseCase;
import com.edutech.student.domain.port.out.AcademicRecordRepository;
import com.edutech.student.domain.port.out.StudentEventPublisher;
import com.edutech.student.domain.port.out.StudentProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AcademicRecordService implements AddAcademicRecordUseCase {

    private static final Logger log = LoggerFactory.getLogger(AcademicRecordService.class);

    private final StudentProfileRepository profileRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final StudentEventPublisher eventPublisher;

    public AcademicRecordService(StudentProfileRepository profileRepository,
                                  AcademicRecordRepository academicRecordRepository,
                                  StudentEventPublisher eventPublisher) {
        this.profileRepository = profileRepository;
        this.academicRecordRepository = academicRecordRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AcademicRecordResponse addRecord(UUID studentId, AddAcademicRecordRequest request) {
        profileRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        AcademicRecord record = AcademicRecord.create(
                studentId,
                request.academicYear(),
                request.classGrade(),
                request.board(),
                request.percentageScore(),
                request.cgpa(),
                null
        );

        AcademicRecord saved = academicRecordRepository.save(record);
        log.info("Academic record added: id={} studentId={}", saved.getId(), studentId);

        eventPublisher.publish(new AcademicRecordAddedEvent(
                UUID.randomUUID().toString(),
                studentId,
                saved.getId(),
                saved.getClassGrade(),
                saved.getPercentageScore(),
                Instant.now()
        ));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AcademicRecordResponse> getRecordsByStudentId(UUID studentId) {
        profileRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        return academicRecordRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AcademicRecordResponse> getRecordsByStudentId(UUID studentId, Pageable pageable) {
        profileRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        List<AcademicRecordResponse> all = academicRecordRepository.findByStudentId(studentId).stream()
                .map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    private AcademicRecordResponse toResponse(AcademicRecord r) {
        return new AcademicRecordResponse(
                r.getId(),
                r.getStudentId(),
                r.getAcademicYear(),
                r.getClassGrade(),
                r.getBoard(),
                r.getPercentageScore(),
                r.getCgpa(),
                Collections.emptyList(),
                r.getCreatedAt()
        );
    }
}
