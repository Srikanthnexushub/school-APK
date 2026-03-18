// src/main/java/com/edutech/center/application/service/StaffService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CreateStaffRequest;
import com.edutech.center.application.dto.TeacherResponse;
import com.edutech.center.application.dto.UpdateStaffRequest;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.application.exception.TeacherAlreadyAssignedException;
import com.edutech.center.application.exception.TeacherNotFoundException;
import com.edutech.center.domain.event.TeacherInvitationEvent;
import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.model.StaffRoleType;
import com.edutech.center.domain.model.Teacher;
import com.edutech.center.domain.model.TeacherStatus;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Staff management service — role-aware creation, profile updates, deactivation,
 * and filtered listing of center staff members.
 *
 * <p>All write operations go through admin access validation. Listing operations
 * support optional filtering by {@link StaffRoleType} and {@link TeacherStatus}.
 */
@Service
public class StaffService {

    private static final Logger log = LoggerFactory.getLogger(StaffService.class);
    private static final long INVITATION_TOKEN_VALIDITY_DAYS = 7;

    private final TeacherRepository teacherRepository;
    private final CenterRepository  centerRepository;
    private final CenterEventPublisher eventPublisher;

    public StaffService(TeacherRepository teacherRepository,
                        CenterRepository centerRepository,
                        CenterEventPublisher eventPublisher) {
        this.teacherRepository = teacherRepository;
        this.centerRepository  = centerRepository;
        this.eventPublisher    = eventPublisher;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    /**
     * Admin creates a staff member by invitation — generates an invitation token
     * and publishes a {@link TeacherInvitationEvent} so the notification service
     * sends an email.  The staff record starts in {@code INVITATION_SENT} status
     * and transitions to {@code ACTIVE} when the staff member accepts.
     */
    @Transactional
    public TeacherResponse createStaff(UUID centerId, CreateStaffRequest req, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        CoachingCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new CenterNotFoundException(centerId));

        if (teacherRepository.existsByEmailAndCenterId(req.email(), centerId)) {
            throw new TeacherAlreadyAssignedException(req.email(), centerId);
        }

        String token      = UUID.randomUUID().toString();
        Instant tokenExpiry = Instant.now().plus(INVITATION_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);

        Teacher staff = Teacher.createStaffInvitation(
                centerId,
                req.firstName(), req.lastName(), req.email(), req.phoneNumber(),
                req.subjects(), req.district(), req.employeeId(),
                req.roleType(), req.qualification(), req.yearsOfExperience(),
                req.designation(), req.bio(),
                token, tokenExpiry);

        Teacher saved = teacherRepository.save(staff);

        eventPublisher.publish(new TeacherInvitationEvent(
                saved.getId(), centerId, center.getName(),
                saved.getEmail(), saved.getFirstName(), saved.getLastName(),
                saved.getInvitationToken()));

        log.info("Staff invitation created: staffId={} centerId={} roleType={}",
                saved.getId(), centerId, req.roleType());
        return toResponse(saved);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    /**
     * Partial profile update (PATCH semantics).
     * Only non-null fields in {@link UpdateStaffRequest} are applied.
     */
    @Transactional
    public TeacherResponse updateStaff(UUID centerId, UUID staffId,
                                       UpdateStaffRequest req, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        Teacher staff = teacherRepository.findByIdAndCenterId(staffId, centerId)
                .orElseThrow(() -> new TeacherNotFoundException(staffId));

        staff.updateProfile(
                req.firstName(), req.lastName(), req.phoneNumber(),
                req.roleType(), req.designation(), req.subjects(),
                req.district(), req.qualification(), req.yearsOfExperience(), req.bio());

        Teacher saved = teacherRepository.save(staff);
        log.info("Staff profile updated: staffId={} centerId={}", staffId, centerId);
        return toResponse(saved);
    }

    // ─── Deactivate ───────────────────────────────────────────────────────────

    /**
     * Soft-deletes a staff member (sets status to INACTIVE, stamps deletedAt).
     * Only CENTER_ADMIN or SUPER_ADMIN may deactivate.
     */
    @Transactional
    public TeacherResponse deactivateStaff(UUID centerId, UUID staffId, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        Teacher staff = teacherRepository.findByIdAndCenterId(staffId, centerId)
                .orElseThrow(() -> new TeacherNotFoundException(staffId));

        staff.deactivate();
        Teacher saved = teacherRepository.save(staff);
        log.info("Staff deactivated: staffId={} centerId={}", staffId, centerId);
        return toResponse(saved);
    }

    /**
     * Re-activates a previously deactivated staff member.
     */
    @Transactional
    public TeacherResponse reactivateStaff(UUID centerId, UUID staffId, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        // findById (not filtered) so we can reactivate soft-deleted records
        Teacher staff = teacherRepository.findById(staffId)
                .filter(t -> t.getCenterId().equals(centerId))
                .orElseThrow(() -> new TeacherNotFoundException(staffId));

        staff.reactivate();
        Teacher saved = teacherRepository.save(staff);
        log.info("Staff reactivated: staffId={} centerId={}", staffId, centerId);
        return toResponse(saved);
    }

    // ─── List ─────────────────────────────────────────────────────────────────

    /**
     * Returns all non-deleted staff for the center, with optional in-memory
     * filtering by {@code roleType} and/or {@code status}.
     * The dataset per center is small enough that DB-level filtering would add
     * complexity without meaningful performance gain.
     */
    @Transactional(readOnly = true)
    public List<TeacherResponse> listStaff(UUID centerId, AuthPrincipal principal,
                                           StaffRoleType roleType, TeacherStatus status) {
        assertAdminAccess(centerId, principal);
        return teacherRepository.findByCenterId(centerId).stream()
                .filter(t -> roleType == null || roleType.equals(t.getRoleType()))
                .filter(t -> status  == null || status.equals(t.getStatus()))
                .map(this::toResponse)
                .toList();
    }

    // ─── Access control ───────────────────────────────────────────────────────

    private void assertAdminAccess(UUID centerId, AuthPrincipal principal) {
        if (principal.isSuperAdmin()) return;
        if (principal.belongsToCenter(centerId)) return;
        boolean isOwningAdmin = centerRepository.findById(centerId)
                .map(c -> principal.belongsToCenter(centerId, c.getAdminUserId()))
                .orElse(false);
        if (!isOwningAdmin) throw new CenterAccessDeniedException();
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private TeacherResponse toResponse(Teacher t) {
        return new TeacherResponse(
                t.getId(), t.getCenterId(), t.getUserId(),
                t.getFirstName(), t.getLastName(), t.getEmail(),
                t.getPhoneNumber(), t.getSubjects(), t.getDistrict(), t.getEmployeeId(),
                t.getStatus(), t.getJoinedAt(),
                t.getRoleType(), t.getQualification(), t.getYearsOfExperience(),
                t.getDesignation(), t.getBio());
    }
}
