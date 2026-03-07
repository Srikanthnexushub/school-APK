package com.edutech.performance.application.service;

import com.edutech.performance.application.dto.SubjectMasteryResponse;
import com.edutech.performance.domain.model.SubjectMastery;
import com.edutech.performance.domain.port.in.GetSubjectMasteryUseCase;
import com.edutech.performance.domain.port.in.UpdateSubjectMasteryUseCase;
import com.edutech.performance.domain.port.out.SubjectMasteryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SubjectMasteryService implements UpdateSubjectMasteryUseCase, GetSubjectMasteryUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubjectMasteryService.class);

    private final SubjectMasteryRepository subjectMasteryRepository;

    public SubjectMasteryService(SubjectMasteryRepository subjectMasteryRepository) {
        this.subjectMasteryRepository = subjectMasteryRepository;
    }

    @Override
    @Transactional
    public SubjectMasteryResponse updateMastery(UUID studentId, UUID enrollmentId,
                                                 String subject, BigDecimal newMasteryPercent) {
        SubjectMastery mastery = subjectMasteryRepository
                .findByStudentIdAndEnrollmentIdAndSubject(studentId, enrollmentId, subject)
                .orElseGet(() -> SubjectMastery.create(studentId, enrollmentId, subject));

        mastery.updateMastery(newMasteryPercent);
        SubjectMastery saved = subjectMasteryRepository.save(mastery);

        log.info("Updated mastery for studentId={} subject={} mastery={}",
                studentId, subject, newMasteryPercent);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectMasteryResponse> getSubjectMastery(UUID studentId, UUID enrollmentId) {
        return subjectMasteryRepository.findByStudentIdAndEnrollmentId(studentId, enrollmentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private SubjectMasteryResponse toResponse(SubjectMastery mastery) {
        return new SubjectMasteryResponse(
                mastery.getId(),
                mastery.getStudentId(),
                mastery.getSubject(),
                mastery.getMasteryPercent(),
                mastery.getMasteryLevel(),
                mastery.getVelocityPerWeek(),
                mastery.getTotalTopics(),
                mastery.getMasteredTopics(),
                mastery.getLastUpdatedAt()
        );
    }
}
