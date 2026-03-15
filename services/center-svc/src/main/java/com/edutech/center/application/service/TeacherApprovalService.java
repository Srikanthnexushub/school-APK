// src/main/java/com/edutech/center/application/service/TeacherApprovalService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.TeacherResponse;
import com.edutech.center.application.dto.TeacherSelfRegisterRequest;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.application.exception.TeacherAlreadyAssignedException;
import com.edutech.center.application.exception.TeacherNotFoundException;
import com.edutech.center.domain.event.TeacherPendingApprovalEvent;
import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.model.Teacher;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeacherApprovalService {

    private static final Logger log = LoggerFactory.getLogger(TeacherApprovalService.class);

    private final TeacherRepository teacherRepository;
    private final CenterRepository centerRepository;
    private final CenterEventPublisher eventPublisher;

    public TeacherApprovalService(TeacherRepository teacherRepository,
                                   CenterRepository centerRepository,
                                   CenterEventPublisher eventPublisher) {
        this.teacherRepository = teacherRepository;
        this.centerRepository = centerRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * A TEACHER user self-registers to a center after creating their auth account.
     * Creates a PENDING_APPROVAL teacher record.
     */
    @Transactional
    public TeacherResponse selfRegister(UUID centerId, TeacherSelfRegisterRequest req, AuthPrincipal principal) {
        CoachingCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new CenterNotFoundException(centerId));

        if (teacherRepository.existsByUserIdAndCenterId(principal.userId(), centerId)) {
            throw new TeacherAlreadyAssignedException(principal.userId(), centerId);
        }

        Teacher teacher = Teacher.createPending(centerId, principal.userId(),
            req.firstName(), req.lastName(), req.email(), req.phoneNumber(), req.subjects(), req.district());
        Teacher saved = teacherRepository.save(teacher);

        eventPublisher.publish(new TeacherPendingApprovalEvent(
            saved.getId(), centerId, center.getName(),
            saved.getFirstName(), saved.getLastName(), saved.getEmail(), saved.getSubjects()));

        log.info("Teacher self-registered pending approval: teacherId={} centerId={}", saved.getId(), centerId);
        return toResponse(saved);
    }

    /** List teachers awaiting approval for this center (CENTER_ADMIN / SUPER_ADMIN only). */
    @Transactional(readOnly = true)
    public List<TeacherResponse> listPending(UUID centerId, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        return teacherRepository.findPendingByCenterId(centerId).stream().map(this::toResponse).toList();
    }

    /** Approve a self-registered teacher. */
    @Transactional
    public TeacherResponse approve(UUID centerId, UUID teacherId, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        Teacher teacher = teacherRepository.findByIdAndCenterId(teacherId, centerId)
                .orElseThrow(() -> new TeacherNotFoundException(teacherId));
        teacher.approve();
        Teacher saved = teacherRepository.save(teacher);
        log.info("Teacher approved: teacherId={} centerId={}", teacherId, centerId);
        return toResponse(saved);
    }

    /** Reject a self-registered teacher. */
    @Transactional
    public TeacherResponse reject(UUID centerId, UUID teacherId, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        Teacher teacher = teacherRepository.findByIdAndCenterId(teacherId, centerId)
                .orElseThrow(() -> new TeacherNotFoundException(teacherId));
        teacher.reject();
        Teacher saved = teacherRepository.save(teacher);
        log.info("Teacher rejected: teacherId={} centerId={}", teacherId, centerId);
        return toResponse(saved);
    }

    private void assertAdminAccess(UUID centerId, AuthPrincipal principal) {
        if (principal.isSuperAdmin()) return;
        if (principal.belongsToCenter(centerId)) return;
        // Allow CENTER_ADMINs whose JWT centerId is still null (Kafka sync pending).
        boolean isOwningAdmin = centerRepository.findById(centerId)
                .map(c -> principal.belongsToCenter(centerId, c.getAdminUserId()))
                .orElse(false);
        if (!isOwningAdmin) throw new CenterAccessDeniedException();
    }

    private TeacherResponse toResponse(Teacher t) {
        return new TeacherResponse(t.getId(), t.getCenterId(), t.getUserId(),
            t.getFirstName(), t.getLastName(), t.getEmail(),
            t.getPhoneNumber(), t.getSubjects(), t.getDistrict(), t.getEmployeeId(),
            t.getStatus(), t.getJoinedAt());
    }
}
