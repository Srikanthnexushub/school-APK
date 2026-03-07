package com.edutech.performance.infrastructure.persistence;

import com.edutech.performance.domain.model.WeakAreaRecord;
import com.edutech.performance.domain.port.out.WeakAreaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class WeakAreaPersistenceAdapter implements WeakAreaRepository {

    private final SpringDataWeakAreaRepository springData;

    public WeakAreaPersistenceAdapter(SpringDataWeakAreaRepository springData) {
        this.springData = springData;
    }

    @Override
    public WeakAreaRecord save(WeakAreaRecord record) {
        return springData.save(record);
    }

    @Override
    public Optional<WeakAreaRecord> findById(UUID id) {
        return springData.findById(id);
    }

    @Override
    public List<WeakAreaRecord> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId) {
        return springData.findByStudentIdAndEnrollmentId(studentId, enrollmentId);
    }

    @Override
    public List<WeakAreaRecord> findByStudentIdOrderByMasteryAsc(UUID studentId, int limit) {
        return springData.findByStudentIdOrderByMasteryAsc(studentId, limit);
    }

    @Override
    public List<WeakAreaRecord> findByStudentIdAndSubject(UUID studentId, String subject) {
        return springData.findByStudentIdAndSubject(studentId, subject);
    }
}
