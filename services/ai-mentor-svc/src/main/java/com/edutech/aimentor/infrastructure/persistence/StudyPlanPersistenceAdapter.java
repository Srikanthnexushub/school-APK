package com.edutech.aimentor.infrastructure.persistence;

import com.edutech.aimentor.domain.model.StudyPlan;
import com.edutech.aimentor.domain.model.StudyPlanItem;
import com.edutech.aimentor.domain.port.out.StudyPlanRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class StudyPlanPersistenceAdapter implements StudyPlanRepository {

    private final SpringDataStudyPlanRepository studyPlanRepo;
    private final SpringDataStudyPlanItemRepository studyPlanItemRepo;

    public StudyPlanPersistenceAdapter(SpringDataStudyPlanRepository studyPlanRepo,
                                        SpringDataStudyPlanItemRepository studyPlanItemRepo) {
        this.studyPlanRepo = studyPlanRepo;
        this.studyPlanItemRepo = studyPlanItemRepo;
    }

    @Override
    public StudyPlan save(StudyPlan studyPlan) {
        return studyPlanRepo.save(studyPlan);
    }

    @Override
    public Optional<StudyPlan> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId) {
        return studyPlanRepo.findByStudentIdAndEnrollmentIdAndDeletedAtIsNull(studentId, enrollmentId);
    }

    @Override
    public Optional<StudyPlan> findById(UUID id) {
        return studyPlanRepo.findById(id);
    }

    @Override
    public Optional<StudyPlanItem> findItemById(UUID itemId) {
        return studyPlanItemRepo.findByIdAndDeletedAtIsNull(itemId);
    }

    @Override
    public StudyPlanItem saveItem(StudyPlanItem item) {
        return studyPlanItemRepo.save(item);
    }

    @Override
    public List<StudyPlan> findAllByStudentId(UUID studentId) {
        return studyPlanRepo.findAllByStudentIdAndDeletedAtIsNull(studentId);
    }
}
