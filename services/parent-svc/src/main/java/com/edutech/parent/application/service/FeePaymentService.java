// src/main/java/com/edutech/parent/application/service/FeePaymentService.java
package com.edutech.parent.application.service;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.FeePaymentResponse;
import com.edutech.parent.application.dto.RecordFeePaymentRequest;
import com.edutech.parent.application.exception.ParentAccessDeniedException;
import com.edutech.parent.application.exception.ParentProfileNotFoundException;
import com.edutech.parent.domain.event.FeePaymentRecordedEvent;
import com.edutech.parent.domain.model.FeePayment;
import com.edutech.parent.domain.model.ParentProfile;
import com.edutech.parent.domain.port.in.RecordFeePaymentUseCase;
import com.edutech.parent.domain.port.out.FeePaymentRepository;
import com.edutech.parent.domain.port.out.ParentEventPublisher;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FeePaymentService implements RecordFeePaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(FeePaymentService.class);

    private final FeePaymentRepository paymentRepository;
    private final ParentProfileRepository profileRepository;
    private final ParentEventPublisher eventPublisher;

    public FeePaymentService(FeePaymentRepository paymentRepository,
                              ParentProfileRepository profileRepository,
                              ParentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.profileRepository = profileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public FeePaymentResponse recordPayment(UUID parentProfileId, RecordFeePaymentRequest request, AuthPrincipal principal) {
        ParentProfile parent = profileRepository.findById(parentProfileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(parentProfileId));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        String currency = (request.currency() != null && !request.currency().isBlank()) ? request.currency() : "INR";
        FeePayment payment = FeePayment.create(
                parentProfileId,
                request.studentId(),
                request.centerId(),
                request.batchId(),
                request.amountPaid(),
                currency,
                request.paymentDate(),
                request.referenceNumber(),
                request.remarks(),
                request.feeType(),
                request.paymentMethod()
        );
        FeePayment saved = paymentRepository.save(payment);
        eventPublisher.publish(new FeePaymentRecordedEvent(
                saved.getId(),
                parentProfileId,
                request.studentId(),
                request.centerId(),
                request.batchId(),
                saved.getAmountPaid(),
                currency
        ));
        log.info("Fee payment recorded: id={} parentId={} amount={}", saved.getId(), parentProfileId, saved.getAmountPaid());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FeePaymentResponse> listPayments(UUID parentProfileId, AuthPrincipal principal) {
        ParentProfile parent = profileRepository.findById(parentProfileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(parentProfileId));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        return paymentRepository.findByParentId(parentProfileId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<FeePaymentResponse> listPayments(UUID parentProfileId, AuthPrincipal principal, Pageable pageable) {
        ParentProfile parent = profileRepository.findById(parentProfileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(parentProfileId));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        List<FeePaymentResponse> all = paymentRepository.findByParentId(parentProfileId).stream()
                .map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    private FeePaymentResponse toResponse(FeePayment p) {
        return new FeePaymentResponse(
                p.getId(),
                p.getParentId(),
                p.getStudentId(),
                p.getCenterId(),
                p.getBatchId(),
                p.getAmountPaid(),
                p.getCurrency(),
                p.getPaymentDate(),
                p.getReferenceNumber(),
                p.getRemarks(),
                p.getFeeType(),
                p.getPaymentMethod(),
                p.getStatus(),
                p.getCreatedAt()
        );
    }
}
