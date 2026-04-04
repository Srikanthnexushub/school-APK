// src/main/java/com/edutech/center/application/service/BatchMemberService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AddBatchMemberRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchMemberResponse;
import com.edutech.center.application.exception.BatchNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.model.BatchMember;
import com.edutech.center.domain.port.in.ListBatchMembersUseCase;
import com.edutech.center.domain.port.out.BatchMemberRepository;
import com.edutech.center.domain.port.out.BatchRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BatchMemberService implements ListBatchMembersUseCase {

    private static final Logger log = LoggerFactory.getLogger(BatchMemberService.class);

    private final BatchMemberRepository memberRepository;
    private final BatchRepository batchRepository;
    private final TeacherRepository teacherRepository;

    public BatchMemberService(BatchMemberRepository memberRepository,
                              BatchRepository batchRepository,
                              TeacherRepository teacherRepository) {
        this.memberRepository = memberRepository;
        this.batchRepository = batchRepository;
        this.teacherRepository = teacherRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchMemberResponse> listMembers(UUID batchId, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));
        boolean canView = principal.belongsToCenter(batch.getCenterId())
                || principal.isSuperAdmin() || principal.isInstitutionAdmin()
                || principal.isParent()
                || (principal.isTeacher() && teacherRepository.existsByUserIdAndCenterId(principal.userId(), batch.getCenterId()));
        if (!canView) {
            throw new CenterAccessDeniedException();
        }
        return memberRepository.findActiveByBatchId(batchId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public BatchMemberResponse addMember(UUID batchId, AddBatchMemberRequest request, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));
        // Students and parents may not add batch members
        if (principal.isStudent() || principal.role() == com.edutech.center.domain.model.Role.PARENT) {
            throw new CenterAccessDeniedException();
        }
        if (!principal.belongsToCenter(batch.getCenterId()) && !principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
            throw new CenterAccessDeniedException();
        }
        Optional<BatchMember> existing = memberRepository.findByBatchIdAndStudentId(batchId, request.studentId());

        final BatchMember member;
        final boolean incrementCount;

        if (existing.isEmpty()) {
            // Brand-new enrollment
            member = BatchMember.enroll(batchId, batch.getCenterId(), request.studentId(), request.studentName());
            incrementCount = true;
        } else if (!existing.get().isActive()) {
            // Previously withdrawn — rejoin same row (avoids unique constraint violation)
            BatchMember withdrawn = existing.get();
            withdrawn.rejoin();
            member = withdrawn;
            incrementCount = true;
        } else {
            // Already actively enrolled — idempotent, no count change
            member = existing.get();
            incrementCount = false;
        }

        BatchMember saved = memberRepository.save(member);
        if (incrementCount) {
            batch.incrementEnrollment();
            batchRepository.save(batch);
        }
        log.info("BatchMember added: batchId={} studentId={} incrementCount={}", batchId, request.studentId(), incrementCount);
        return toResponse(saved);
    }

    @Transactional
    public void withdrawMember(UUID batchId, UUID studentId, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));
        if (!principal.belongsToCenter(batch.getCenterId()) && !principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
            throw new CenterAccessDeniedException();
        }
        memberRepository.findByBatchIdAndStudentId(batchId, studentId).ifPresent(m -> {
            if (!m.isActive()) return; // already withdrawn — nothing to do
            m.withdraw();
            memberRepository.save(m);
            batch.decrementEnrollment();
            batchRepository.save(batch);
            log.info("BatchMember withdrawn: batchId={} studentId={}", batchId, studentId);
        });
    }

    @Transactional(readOnly = true)
    public List<BatchMemberResponse> listByStudentId(UUID studentId) {
        return memberRepository.findByStudentId(studentId).stream()
                .map(this::toResponse).toList();
    }

    private BatchMemberResponse toResponse(BatchMember m) {
        return new BatchMemberResponse(m.getId(), m.getBatchId(), m.getCenterId(),
                m.getStudentId(), m.getStudentName(), m.getEnrolledAt(),
                m.getWithdrawnAt(), m.isActive());
    }
}
