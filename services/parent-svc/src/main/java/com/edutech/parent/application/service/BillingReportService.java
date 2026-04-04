// src/main/java/com/edutech/parent/application/service/BillingReportService.java
package com.edutech.parent.application.service;

import com.edutech.parent.application.dto.AdminRecordPaymentRequest;
import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.FeePaymentResponse;
import com.edutech.parent.application.dto.StudentFeeReportItem;
import com.edutech.parent.application.exception.ParentAccessDeniedException;
import com.edutech.parent.domain.event.FeePaymentRecordedEvent;
import com.edutech.parent.domain.model.FeePayment;
import com.edutech.parent.domain.model.PaymentStatus;
import com.edutech.parent.domain.model.Role;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.FeePaymentRepository;
import com.edutech.parent.domain.port.out.NotificationPublisher;
import com.edutech.parent.domain.port.out.ParentEventPublisher;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BillingReportService {

    private static final Logger log = LoggerFactory.getLogger(BillingReportService.class);

    private final StudentLinkRepository studentLinkRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final NotificationPublisher notificationPublisher;
    private final ParentEventPublisher eventPublisher;

    public BillingReportService(StudentLinkRepository studentLinkRepository,
                                 FeePaymentRepository feePaymentRepository,
                                 ParentProfileRepository parentProfileRepository,
                                 NotificationPublisher notificationPublisher,
                                 ParentEventPublisher eventPublisher) {
        this.studentLinkRepository = studentLinkRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.notificationPublisher = notificationPublisher;
        this.eventPublisher = eventPublisher;
    }

    public List<StudentFeeReportItem> getBillingReport(UUID centerId, AuthPrincipal principal) {
        Objects.requireNonNull(centerId, "centerId must not be null");
        Objects.requireNonNull(principal, "principal must not be null");
        requireAdminAccess(centerId, principal);

        List<StudentLink> links = studentLinkRepository.findActiveByCenterId(centerId);
        List<FeePayment> allPayments = feePaymentRepository.findByCenterId(centerId);

        Map<UUID, List<FeePayment>> byStudent = allPayments.stream()
                .collect(Collectors.groupingBy(FeePayment::getStudentId));

        List<StudentFeeReportItem> report = new ArrayList<>();
        for (StudentLink link : links) {
            List<FeePayment> studentPayments = byStudent.getOrDefault(link.getStudentId(), List.of());
            BigDecimal totalPaid = studentPayments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.CONFIRMED)
                    .map(FeePayment::getAmountPaid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            report.add(new StudentFeeReportItem(
                    link.getStudentId(),
                    link.getStudentName(),
                    totalPaid,
                    studentPayments.size(),
                    deriveStatus(studentPayments)
            ));
        }
        return report;
    }

    @Transactional
    public void sendFeeReminder(UUID centerId, UUID studentId, AuthPrincipal principal) {
        Objects.requireNonNull(centerId,  "centerId must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(principal, "principal must not be null");
        requireAdminAccess(centerId, principal);

        List<StudentLink> links = studentLinkRepository.findActiveByStudentId(studentId);
        for (StudentLink link : links) {
            if (!centerId.equals(link.getCenterId())) continue;
            parentProfileRepository.findById(link.getParentId()).ifPresent(profile -> {
                String studentName = link.getStudentName() != null ? link.getStudentName() : "your child";
                String body = "Fee payment reminder for " + studentName
                        + ". Please ensure any outstanding fees are settled. "
                        + "Visit the Fees section on your dashboard for details.";
                notificationPublisher.sendInApp(
                        profile.getUserId(),
                        "Fee Payment Reminder",
                        body,
                        Map.of("notificationType", "FEE_REMINDER",
                               "actionUrl",        "/parent/fees")
                );
                log.info("Fee reminder queued: parentUserId={} studentId={}", profile.getUserId(), studentId);
            });
        }
    }

    /**
     * Admin-initiated payment recording.
     * Looks up the parent profile from StudentLink, creates a CONFIRMED FeePayment,
     * and publishes the FeePaymentRecordedEvent (so parents receive in-app notification).
     *
     * <p>CENTER_ADMIN can only record for their own center.
     * INSTITUTION_ADMIN and SUPER_ADMIN can record for any center.</p>
     */
    @Transactional
    public FeePaymentResponse recordPaymentForAdmin(AdminRecordPaymentRequest request, AuthPrincipal principal) {
        Objects.requireNonNull(request,   "request must not be null");
        Objects.requireNonNull(principal, "principal must not be null");
        requireAdminAccess(request.centerId(), principal);

        // Resolve parent profile from the student link
        StudentLink link = studentLinkRepository.findActiveByStudentId(request.studentId()).stream()
                .filter(l -> request.centerId().equals(l.getCenterId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student " + request.studentId() + " is not actively enrolled in center " + request.centerId()));

        UUID parentProfileId = link.getParentId();
        String currency      = "INR";
        String method        = request.paymentMethod() != null && !request.paymentMethod().isBlank()
                               ? request.paymentMethod() : "CASH";
        String feeType       = request.feeType() != null && !request.feeType().isBlank()
                               ? request.feeType() : "TUITION";

        FeePayment payment = FeePayment.create(
                parentProfileId, request.studentId(), request.centerId(), null,
                request.amountPaid(), currency, request.paymentDate(),
                request.referenceNumber(), request.remarks(), feeType, method);

        // Admin-recorded payments are immediately CONFIRMED
        payment.confirm();
        FeePayment saved = feePaymentRepository.save(payment);

        eventPublisher.publish(new FeePaymentRecordedEvent(
                saved.getId(), parentProfileId, request.studentId(),
                request.centerId(), null, saved.getAmountPaid(), currency));

        log.info("Admin recorded fee payment: studentId={} centerId={} amount={}",
                request.studentId(), request.centerId(), request.amountPaid());
        return toResponse(saved);
    }

    private FeePaymentResponse toResponse(FeePayment p) {
        return new FeePaymentResponse(p.getId(), p.getParentId(), p.getStudentId(), p.getCenterId(),
                p.getBatchId(), p.getAmountPaid(), p.getCurrency(), p.getPaymentDate(),
                p.getReferenceNumber(), p.getRemarks(), p.getFeeType(), p.getPaymentMethod(),
                p.getStatus(), p.getCreatedAt());
    }

    private void requireAdminAccess(UUID centerId, AuthPrincipal principal) {
        if (principal.isSuperAdmin() || principal.isInstitutionAdmin()) return;
        if (principal.role() == Role.CENTER_ADMIN) {
            if (!centerId.equals(principal.centerId())) throw new ParentAccessDeniedException();
            return;
        }
        throw new ParentAccessDeniedException();
    }

    private String deriveStatus(List<FeePayment> payments) {
        if (payments == null || payments.isEmpty()) return "NO_PAYMENT";
        boolean hasConfirmed = payments.stream().anyMatch(p -> p.getStatus() == PaymentStatus.CONFIRMED);
        boolean hasPending   = payments.stream().anyMatch(p -> p.getStatus() == PaymentStatus.PENDING);
        // Only DISPUTED/REFUNDED payments remaining — net amount paid is zero
        if (!hasConfirmed && !hasPending) return "NO_PAYMENT";
        if (hasConfirmed && !hasPending)  return "FULLY_PAID";
        return "PARTIAL";
    }
}
