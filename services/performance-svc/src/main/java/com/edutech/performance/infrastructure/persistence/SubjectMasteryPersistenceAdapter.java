package com.edutech.performance.infrastructure.persistence;

import com.edutech.performance.domain.model.SubjectMastery;
import com.edutech.performance.domain.port.out.SubjectMasteryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubjectMasteryPersistenceAdapter implements SubjectMasteryRepository {

    private final SpringDataSubjectMasteryRepository springData;

    public SubjectMasteryPersistenceAdapter(SpringDataSubjectMasteryRepository springData) {
        this.springData = springData;
    }

    @Override
    public SubjectMastery save(SubjectMastery mastery) {
        return springData.save(mastery);
    }

    @Override
    public List<SubjectMastery> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId) {
        return springData.findByStudentIdAndEnrollmentId(studentId, enrollmentId);
    }

    @Override
    public Optional<SubjectMastery> findByStudentIdAndEnrollmentIdAndSubject(UUID studentId, UUID enrollmentId, String subject) {
        return springData.findByStudentIdAndEnrollmentIdAndSubject(studentId, enrollmentId, subject);
    }
}
