// src/main/java/com/edutech/parent/application/service/InternalParentQueryService.java
package com.edutech.parent.application.service;

import com.edutech.parent.domain.model.ParentProfile;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for internal (service-to-service) queries on parent data.
 * All methods are read-only and do not enforce user-level auth —
 * they are called only from controllers secured by X-Service-Key.
 */
@Service
public class InternalParentQueryService {

    private static final Logger log = LoggerFactory.getLogger(InternalParentQueryService.class);

    private final ParentProfileRepository profileRepository;
    private final StudentLinkRepository linkRepository;

    public InternalParentQueryService(ParentProfileRepository profileRepository,
                                      StudentLinkRepository linkRepository) {
        this.profileRepository = profileRepository;
        this.linkRepository = linkRepository;
    }

    /**
     * Returns true if an ACTIVE student-link exists between the given parent userId
     * and the given student userId.
     *
     * This is the ONLY correct service-to-service link check.
     * Called by psych-svc (and any other service) via the internal endpoint
     * GET /api/v1/internal/parents/{parentUserId}/is-linked-to-student/{studentId}.
     *
     * @param parentUserId the parent's auth userId (JWT sub)
     * @param studentId    the student whose data is being requested
     * @return true iff an ACTIVE link exists; false if no profile or no active link
     */
    @Transactional(readOnly = true)
    public boolean isParentLinkedToStudent(UUID parentUserId, UUID studentId) {
        return profileRepository.findByUserId(parentUserId)
                .map(profile -> linkRepository.findByParentIdAndStudentId(profile.getId(), studentId).isPresent())
                .orElse(false);
    }

    /**
     * Returns the distinct set of center IDs where the given parent userId
     * has at least one active student link.
     *
     * @param parentUserId the parent's auth userId (JWT sub)
     * @return list of center UUIDs; empty if no profile or no active links
     */
    @Transactional(readOnly = true)
    public List<UUID> getLinkedCenterIds(UUID parentUserId) {
        return profileRepository.findByUserId(parentUserId)
                .map(ParentProfile::getId)
                .map(profileId -> {
                    List<StudentLink> activeLinks = linkRepository.findActiveByParentId(profileId);
                    List<UUID> centerIds = activeLinks.stream()
                            .map(StudentLink::getCenterId)
                            .filter(id -> id != null)
                            .distinct()
                            .toList();
                    log.debug("getLinkedCenterIds parentUserId={} -> {} center(s)", parentUserId, centerIds.size());
                    return centerIds;
                })
                .orElseGet(() -> {
                    log.debug("getLinkedCenterIds: no parent profile found for userId={}", parentUserId);
                    return List.of();
                });
    }
}
