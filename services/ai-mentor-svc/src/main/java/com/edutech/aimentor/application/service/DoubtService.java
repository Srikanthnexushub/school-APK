package com.edutech.aimentor.application.service;

import com.edutech.aimentor.application.dto.DoubtTicketResponse;
import com.edutech.aimentor.application.dto.SubmitDoubtRequest;
import com.edutech.aimentor.application.exception.DoubtNotFoundException;
import com.edutech.aimentor.domain.event.DoubtResolvedEvent;
import com.edutech.aimentor.domain.event.DoubtSubmittedEvent;
import com.edutech.aimentor.domain.model.DoubtTicket;
import com.edutech.aimentor.domain.port.in.GetDoubtUseCase;
import com.edutech.aimentor.domain.port.in.SubmitDoubtUseCase;
import com.edutech.aimentor.domain.port.out.AiGatewayClient;
import com.edutech.aimentor.domain.port.out.AiMentorEventPublisher;
import com.edutech.aimentor.domain.port.out.DoubtTicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DoubtService implements SubmitDoubtUseCase, GetDoubtUseCase {

    private static final Logger log = LoggerFactory.getLogger(DoubtService.class);

    private final DoubtTicketRepository doubtTicketRepository;
    private final AiGatewayClient aiGatewayClient;
    private final AiMentorEventPublisher eventPublisher;

    public DoubtService(DoubtTicketRepository doubtTicketRepository,
                        AiGatewayClient aiGatewayClient,
                        AiMentorEventPublisher eventPublisher) {
        this.doubtTicketRepository = doubtTicketRepository;
        this.aiGatewayClient = aiGatewayClient;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public DoubtTicketResponse submitDoubt(SubmitDoubtRequest request) {
        DoubtTicket ticket = DoubtTicket.create(
                request.studentId(),
                request.enrollmentId(),
                request.subjectArea(),
                request.question()
        );

        // Attempt AI resolution — graceful degradation if AI is unavailable
        try {
            String aiAnswer = aiGatewayClient.resolveDoubt(request.question(), request.subjectArea());
            ticket.resolve(aiAnswer);
            log.info("AI resolved doubt for studentId={} subjectArea={}", request.studentId(), request.subjectArea());
        } catch (Exception e) {
            log.warn("AI gateway unavailable, keeping doubt ticket in PENDING state. studentId={} error={}",
                    request.studentId(), e.getMessage(), e);
            // ticket remains in PENDING status — no re-throw
        }

        DoubtTicket saved = doubtTicketRepository.save(ticket);

        try {
            eventPublisher.publishDoubtSubmitted(new DoubtSubmittedEvent(
                    saved.getId(),
                    saved.getStudentId(),
                    saved.getEnrollmentId(),
                    saved.getSubjectArea(),
                    Instant.now()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish DoubtSubmittedEvent for doubtTicketId={}: {}",
                    saved.getId(), e.getMessage(), e);
        }

        if (saved.getResolvedAt() != null) {
            try {
                eventPublisher.publishDoubtResolved(new DoubtResolvedEvent(
                        saved.getId(),
                        saved.getStudentId(),
                        saved.getEnrollmentId(),
                        saved.getResolvedAt(),
                        Instant.now()
                ));
            } catch (Exception e) {
                log.warn("Failed to publish DoubtResolvedEvent for doubtTicketId={}: {}",
                        saved.getId(), e.getMessage(), e);
            }
        }

        log.info("Doubt ticket submitted: id={} studentId={} status={}",
                saved.getId(), saved.getStudentId(), saved.getStatus());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DoubtTicketResponse getDoubt(UUID doubtTicketId, UUID studentId) {
        DoubtTicket ticket = doubtTicketRepository
                .findByIdAndStudentId(doubtTicketId, studentId)
                .orElseThrow(() -> new DoubtNotFoundException(doubtTicketId, studentId));
        return toResponse(ticket);
    }

    private DoubtTicketResponse toResponse(DoubtTicket ticket) {
        return new DoubtTicketResponse(
                ticket.getId(),
                ticket.getStudentId(),
                ticket.getEnrollmentId(),
                ticket.getSubjectArea(),
                ticket.getQuestion(),
                ticket.getAiAnswer(),
                ticket.getStatus(),
                ticket.getResolvedAt(),
                ticket.getCreatedAt()
        );
    }
}
