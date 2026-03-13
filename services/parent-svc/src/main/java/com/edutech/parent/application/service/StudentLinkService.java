// src/main/java/com/edutech/parent/application/service/StudentLinkService.java
package com.edutech.parent.application.service;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.LinkStudentRequest;
import com.edutech.parent.application.dto.StudentLinkResponse;
import com.edutech.parent.application.dto.UpdateChildDetailsRequest;
import com.edutech.parent.application.exception.DuplicateStudentLinkException;
import com.edutech.parent.application.exception.ParentAccessDeniedException;
import com.edutech.parent.application.exception.ParentProfileNotFoundException;
import com.edutech.parent.application.exception.StudentLinkNotFoundException;
import com.edutech.parent.application.exception.TooManyChildrenException;
import com.edutech.parent.application.exception.TooManyParentsException;
import com.edutech.parent.domain.event.LinkRevokedEvent;
import com.edutech.parent.domain.event.StudentLinkedEvent;
import com.edutech.parent.domain.model.LinkStatus;
import com.edutech.parent.domain.model.ParentProfile;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.in.LinkStudentUseCase;
import com.edutech.parent.domain.port.in.RevokeStudentLinkUseCase;
import com.edutech.parent.domain.port.out.ParentEventPublisher;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StudentLinkService implements LinkStudentUseCase, RevokeStudentLinkUseCase {

    private final StudentLinkRepository linkRepository;
    private final ParentProfileRepository profileRepository;
    private final ParentEventPublisher eventPublisher;

    public StudentLinkService(StudentLinkRepository linkRepository,
                               ParentProfileRepository profileRepository,
                               ParentEventPublisher eventPublisher) {
        this.linkRepository = linkRepository;
        this.profileRepository = profileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public StudentLinkResponse linkStudent(UUID parentProfileId, LinkStudentRequest request, AuthPrincipal principal) {
        ParentProfile parent = profileRepository.findById(parentProfileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(parentProfileId));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        linkRepository.findByParentIdAndStudentId(parentProfileId, request.studentId())
                .ifPresent(existing -> {
                    if (existing.getStatus() != LinkStatus.REVOKED) {
                        throw new DuplicateStudentLinkException(request.studentId());
                    }
                });
        // Enforce max 5 children per parent
        long activeChildCount = linkRepository.findActiveByParentId(parentProfileId).size();
        if (activeChildCount >= 5) {
            throw new TooManyChildrenException();
        }
        // Enforce max 2 parents per student
        long activeParentCount = linkRepository.findActiveByStudentId(request.studentId()).size();
        if (activeParentCount >= 2) {
            throw new TooManyParentsException();
        }
        StudentLink link = StudentLink.create(parentProfileId, request.studentId(), request.studentName(), request.centerId(), request.relationship());
        if (request.dateOfBirth() != null || request.schoolName() != null || request.standard() != null
                || request.board() != null || request.rollNumber() != null) {
            link.updateChildDetails(request.dateOfBirth(), request.schoolName(), request.standard(),
                    request.board(), request.rollNumber());
        }
        StudentLink saved = linkRepository.save(link);
        eventPublisher.publish(new StudentLinkedEvent(saved.getId(), parentProfileId, request.studentId(), request.centerId()));
        return toResponse(saved);
    }

    @Override
    public void revokeLink(UUID linkId, AuthPrincipal principal) {
        StudentLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new StudentLinkNotFoundException(linkId));
        ParentProfile parent = profileRepository.findById(link.getParentId())
                .orElseThrow(() -> new ParentProfileNotFoundException(link.getParentId()));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        link.revoke();
        linkRepository.save(link);
        eventPublisher.publish(new LinkRevokedEvent(link.getId(), link.getParentId(), link.getStudentId()));
    }

    @Transactional(readOnly = true)
    public List<StudentLinkResponse> listLinkedStudents(UUID parentProfileId, AuthPrincipal principal) {
        ParentProfile parent = profileRepository.findById(parentProfileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(parentProfileId));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        return linkRepository.findByParentId(parentProfileId).stream()
                .filter(l -> l.getStatus() != LinkStatus.REVOKED)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<StudentLinkResponse> listLinkedStudents(UUID parentProfileId, AuthPrincipal principal, Pageable pageable) {
        ParentProfile parent = profileRepository.findById(parentProfileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(parentProfileId));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        List<StudentLinkResponse> all = linkRepository.findActiveByParentId(parentProfileId).stream()
                .map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    public StudentLinkResponse updateChildDetails(UUID profileId, UUID linkId,
                                                   UpdateChildDetailsRequest request,
                                                   AuthPrincipal principal) {
        ParentProfile parent = profileRepository.findById(profileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(profileId));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        StudentLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new StudentLinkNotFoundException(linkId));
        if (!link.getParentId().equals(profileId)) {
            throw new ParentAccessDeniedException();
        }
        link.updateChildDetails(
                request.dateOfBirth(),
                request.schoolName(),
                request.standard(),
                request.board(),
                request.rollNumber()
        );
        return toResponse(linkRepository.save(link));
    }

    private StudentLinkResponse toResponse(StudentLink l) {
        return new StudentLinkResponse(
                l.getId(),
                l.getParentId(),
                l.getStudentId(),
                l.getStudentName(),
                l.getCenterId(),
                l.getStatus(),
                l.getRelationship(),
                l.getDateOfBirth(),
                l.getSchoolName(),
                l.getStandard(),
                l.getBoard(),
                l.getRollNumber(),
                l.getCreatedAt()
        );
    }
}
