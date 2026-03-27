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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BatchMemberService implements ListBatchMembersUseCase {

    private static final Logger log = LoggerFactory.getLogger(BatchMemberService.class);

    private final BatchMemberRepository memberRepository;
    private final BatchRepository batchRepository;

    public BatchMemberService(BatchMemberRepository memberRepository,
                              BatchRepository batchRepository) {
        this.memberRepository = memberRepository;
        this.batchRepository = batchRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchMemberResponse> listMembers(UUID batchId, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));
        if (!principal.belongsToCenter(batch.getCenterId()) && !principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
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
        // Upsert: if withdrawn, re-enroll; otherwise create new
        BatchMember member = memberRepository.findByBatchIdAndStudentId(batchId, request.studentId())
                .map(m -> { m.withdraw(); return memberRepository.save(m); })
                .orElseGet(() -> BatchMember.enroll(batchId, batch.getCenterId(),
                        request.studentId(), request.studentName()));
        // If this was a re-enroll scenario, create fresh record
        if (member.getWithdrawnAt() != null) {
            member = BatchMember.enroll(batchId, batch.getCenterId(), request.studentId(), request.studentName());
        }
        BatchMember saved = memberRepository.save(member);
        log.info("BatchMember added: batchId={} studentId={}", batchId, request.studentId());
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
            m.withdraw();
            memberRepository.save(m);
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
