// src/main/java/com/edutech/parent/application/service/BillingReportService.java
package com.edutech.parent.application.service;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.StudentFeeReportItem;
import com.edutech.parent.application.exception.ParentAccessDeniedException;
import com.edutech.parent.domain.model.FeePayment;
import com.edutech.parent.domain.model.PaymentStatus;
import com.edutech.parent.domain.model.Role;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.FeePaymentRepository;
import com.edutech.parent.domain.port.out.NotificationPublisher;
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

    public BillingReportService(StudentLinkRepository studentLinkRepository,
                                 FeePaymentRepository feePaymentRepository,
                                 ParentProfileRepository parentProfileRepository,
                                 NotificationPublisher notificationPublisher) {
        this.studentLinkRepository = studentLinkRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.notificationPublisher = notificationPublisher;
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
