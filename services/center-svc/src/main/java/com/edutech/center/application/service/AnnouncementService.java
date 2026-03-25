// src/main/java/com/edutech/center/application/service/AnnouncementService.java
package com.edutech.center.application.service;

import com.edutech.center.application.command.SendAnnouncementCommand;
import com.edutech.center.application.dto.AnnouncementResponse;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.domain.model.Announcement;
import com.edutech.center.domain.model.AnnouncementTargetType;
import com.edutech.center.domain.model.BatchMember;
import com.edutech.center.domain.port.in.SendAnnouncementUseCase;
import com.edutech.center.domain.port.out.AnnouncementRepository;
import com.edutech.center.domain.port.out.BatchMemberRepository;
import com.edutech.center.domain.port.out.NotificationPublisher;
import com.edutech.events.notification.NotificationSendEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnnouncementService implements SendAnnouncementUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final BatchMemberRepository batchMemberRepository;
    private final NotificationPublisher notificationPublisher;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               BatchMemberRepository batchMemberRepository,
                               NotificationPublisher notificationPublisher) {
        this.announcementRepository = announcementRepository;
        this.batchMemberRepository = batchMemberRepository;
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    @Transactional
    public AnnouncementResponse send(SendAnnouncementCommand cmd, AuthPrincipal principal) {
        if (!principal.belongsToCenter(cmd.centerId()) && !principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
            throw new CenterAccessDeniedException();
        }

        Announcement announcement = Announcement.create(
                cmd.centerId(), cmd.title(), cmd.body(),
                principal.userId(), principal.role().name(),
                cmd.targetType(), cmd.targetId(), cmd.targetRole(),
                cmd.scheduledAt(), cmd.expiresAt());

        // Deliver immediately if not scheduled
        if (announcement.isDue()) {
            deliverAnnouncement(announcement, principal);
            announcement.markSent();
        }

        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement created: id={} centerId={} targetType={}", saved.getId(), saved.getCenterId(), saved.getTargetType());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listByCenterId(UUID centerId, AuthPrincipal principal) {
        if (!principal.belongsToCenter(centerId) && !principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
            throw new CenterAccessDeniedException();
        }
        return announcementRepository.findActiveByCenterId(centerId).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void expire(UUID centerId, UUID announcementId, AuthPrincipal principal) {
        if (!principal.belongsToCenter(centerId) && !principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
            throw new CenterAccessDeniedException();
        }
        announcementRepository.findById(announcementId).ifPresent(a -> {
            a.expire();
            announcementRepository.save(a);
        });
    }

    private void deliverAnnouncement(Announcement a, AuthPrincipal principal) {
        List<UUID> recipientIds = resolveRecipients(a);
        Map<String, String> metadata = Map.of(
                "announcementId", a.getId().toString(),
                "centerId", a.getCenterId().toString(),
                "targetType", a.getTargetType().name()
        );
        for (UUID recipientId : recipientIds) {
            NotificationSendEvent event = NotificationSendEvent.inApp(
                    recipientId, a.getTitle(), a.getBody(), metadata);
            notificationPublisher.publish(event);
        }
        log.info("Announcement delivered: id={} recipientCount={}", a.getId(), recipientIds.size());
    }

    private List<UUID> resolveRecipients(Announcement a) {
        if (a.getTargetType() == AnnouncementTargetType.BATCH && a.getTargetId() != null) {
            return batchMemberRepository.findActiveByBatchId(a.getTargetId()).stream()
                    .map(BatchMember::getStudentId).toList();
        }
        // CENTER / ROLE / ALL — in a real system would query all users in center.
        // For now returns empty; full fanout requires cross-service query.
        log.warn("Announcement {} targetType={} fanout not fully implemented for non-BATCH targets",
                a.getId(), a.getTargetType());
        return List.of();
    }

    private AnnouncementResponse toResponse(Announcement a) {
        return new AnnouncementResponse(a.getId(), a.getCenterId(), a.getTitle(), a.getBody(),
                a.getSentById(), a.getSentByRole(), a.getTargetType(), a.getTargetId(),
                a.getTargetRole(), a.getScheduledAt(), a.getSentAt(), a.getExpiresAt(), a.getCreatedAt());
    }
}
