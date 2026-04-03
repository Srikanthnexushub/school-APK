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
import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.model.StaffRoleType;
import com.edutech.center.domain.model.Teacher;
import com.edutech.center.domain.model.TeacherStatus;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import com.edutech.center.infrastructure.config.KafkaTopicProperties;
import com.edutech.events.notification.NotificationSendEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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

    private final TeacherRepository teacherRepository;
    private final CenterRepository  centerRepository;
    private final CenterEventPublisher eventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    public StaffService(TeacherRepository teacherRepository,
                        CenterRepository centerRepository,
                        CenterEventPublisher eventPublisher,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        KafkaTopicProperties topicProperties) {
        this.teacherRepository = teacherRepository;
        this.centerRepository  = centerRepository;
        this.eventPublisher    = eventPublisher;
        this.kafkaTemplate     = kafkaTemplate;
        this.topicProperties   = topicProperties;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    /**
     * Admin creates a staff member by invitation — generates a token, saves the record
     * in {@code INVITATION_SENT} status, and publishes an invitation email via notification-svc.
     * The teacher must click the link to register and complete account activation.
     */
    @Transactional
    public TeacherResponse createStaff(UUID centerId, CreateStaffRequest req, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        CoachingCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new CenterNotFoundException(centerId));

        if (teacherRepository.existsByEmailAndCenterId(req.email(), centerId)) {
            throw new TeacherAlreadyAssignedException(req.email(), centerId);
        }

        String token = UUID.randomUUID().toString();
        Instant tokenExpiry = Instant.now().plus(7, ChronoUnit.DAYS);

        Teacher staff = Teacher.createStaffInvitation(
                centerId,
                req.firstName(), req.lastName(), req.email(), req.phoneNumber(),
                req.subjects(), req.district(), req.employeeId(),
                req.roleType(), req.qualification(), req.yearsOfExperience(),
                req.designation(), req.bio(),
                token, tokenExpiry);

        Teacher saved = teacherRepository.save(staff);
        publishInvitationEmail(saved, center.getName(), token);

        log.info("Staff member created with invitation: staffId={} centerId={} roleType={}",
                saved.getId(), centerId, req.roleType());
        return toResponse(saved);
    }

    /**
     * Resend (or send for the first time) an invitation email to an INVITATION_SENT staff member.
     * Regenerates the token and resets the 7-day expiry window.
     * Used to fix existing records created before invitation emails were wired up.
     */
    @Transactional
    public TeacherResponse resendInvitation(UUID centerId, UUID staffId, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        CoachingCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new CenterNotFoundException(centerId));
        Teacher staff = teacherRepository.findByIdAndCenterId(staffId, centerId)
                .orElseThrow(() -> new TeacherNotFoundException(staffId));

        if (staff.getStatus() != TeacherStatus.INVITATION_SENT) {
            throw new IllegalStateException(
                "Cannot resend invitation — teacher status is " + staff.getStatus());
        }

        String token = UUID.randomUUID().toString();
        Instant tokenExpiry = Instant.now().plus(7, ChronoUnit.DAYS);
        staff.refreshInvitationToken(token, tokenExpiry);
        Teacher saved = teacherRepository.save(staff);
        publishInvitationEmail(saved, center.getName(), token);

        log.info("Invitation resent: staffId={} centerId={}", staffId, centerId);
        return toResponse(saved);
    }

    private void publishInvitationEmail(Teacher teacher, String centerName, String token) {
        String inviteUrl = appBaseUrl + "/accept-invitation?token=" + token;
        String roleLabel = teacher.getRoleType() != null ? teacher.getRoleType().name() : "Staff";
        String subject = "You've been invited to join " + centerName + " on NexusEd";
        String body = "Hi " + teacher.getFirstName() + ",\n\n"
            + "You have been invited to join " + centerName + " as a " + roleLabel + " on NexusEd.\n\n"
            + "Click the link below to set your password and activate your account:\n\n"
            + inviteUrl + "\n\n"
            + "This link expires in 7 days. If you did not expect this invitation, please ignore this email.";

        NotificationSendEvent event = NotificationSendEvent.email(
            null, teacher.getEmail(), subject, body,
            Map.of("teacherId", teacher.getId().toString(), "centerId", teacher.getCenterId().toString()));

        kafkaTemplate.send(topicProperties.notificationSend(), event);
        log.info("Invitation email queued: staffId={} email={}", teacher.getId(), teacher.getEmail());
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
                t.getPhoneNumber(), t.getSubjects(), t.getDistrict(), t.getCountry(), t.getEmployeeId(),
                t.getStatus(), t.getJoinedAt(),
                t.getRoleType(), t.getQualification(), t.getYearsOfExperience(),
                t.getDesignation(), t.getBio());
    }
}
