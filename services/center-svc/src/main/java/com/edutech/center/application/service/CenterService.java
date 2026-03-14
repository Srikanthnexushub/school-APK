// src/main/java/com/edutech/center/application/service/CenterService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterLookupResponse;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.application.dto.InstitutionSelfRegisterRequest;
import com.edutech.center.application.dto.UpdateCenterRequest;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.application.exception.DuplicateCenterCodeException;
import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.port.in.CreateCenterUseCase;
import com.edutech.center.domain.port.in.UpdateCenterUseCase;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CenterService implements CreateCenterUseCase, UpdateCenterUseCase {

    private static final Logger log = LoggerFactory.getLogger(CenterService.class);

    private final CenterRepository centerRepository;
    private final CenterEventPublisher eventPublisher;
    private final TeacherRepository teacherRepository;

    public CenterService(CenterRepository centerRepository,
                         CenterEventPublisher eventPublisher,
                         TeacherRepository teacherRepository) {
        this.centerRepository = centerRepository;
        this.eventPublisher = eventPublisher;
        this.teacherRepository = teacherRepository;
    }

    @Override
    @Transactional
    public CenterResponse createCenter(CreateCenterRequest request, AuthPrincipal principal) {
        if (principal.role() != Role.SUPER_ADMIN) {
            throw new CenterAccessDeniedException();
        }
        if (centerRepository.existsByCode(request.code())) {
            throw new DuplicateCenterCodeException(request.code());
        }

        UUID ownerId = request.ownerId() != null ? request.ownerId() : principal.userId();

        CoachingCenter center = CoachingCenter.create(
            request.name(), request.code(), request.address(),
            request.city(), request.state(), request.pincode(),
            request.phone(), request.email(), request.website(),
            request.logoUrl(), ownerId
        );

        CoachingCenter saved = centerRepository.save(center);
        log.info("Center created: id={} code={}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CenterResponse updateCenter(UUID centerId, UpdateCenterRequest request, AuthPrincipal principal) {
        CoachingCenter center = centerRepository.findById(centerId)
            .orElseThrow(() -> new CenterNotFoundException(centerId));

        if (!principal.isSuperAdmin() && !center.getOwnerId().equals(principal.userId())) {
            throw new CenterAccessDeniedException();
        }

        center.update(request.name(), request.address(), request.city(), request.state(),
                request.pincode(), request.phone(), request.email(),
                request.website(), request.logoUrl());

        CoachingCenter saved = centerRepository.save(center);
        log.info("Center updated: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public CenterResponse selfRegisterCenter(InstitutionSelfRegisterRequest request, AuthPrincipal principal) {
        if (principal.role() != Role.CENTER_ADMIN) {
            throw new CenterAccessDeniedException();
        }
        CoachingCenter center = CoachingCenter.selfRegister(
            request.name(), request.city(), request.phone(),
            principal.email(),
            request.address(), request.state(), request.pincode(),
            principal.userId()
        );
        CoachingCenter saved = centerRepository.save(center);
        log.info("Center self-registered: id={} owner={}", saved.getId(), principal.userId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CenterResponse> listCenters(AuthPrincipal principal) {
        return resolveAccessibleCenters(principal);
    }

    @Transactional(readOnly = true)
    public Page<CenterResponse> listCenters(AuthPrincipal principal, Pageable pageable) {
        List<CenterResponse> all = resolveAccessibleCenters(principal);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    /** Returns all centers the principal may access: owned + teacher-assigned (deduped). */
    private List<CenterResponse> resolveAccessibleCenters(AuthPrincipal principal) {
        if (principal.isSuperAdmin()) {
            return centerRepository.findAll().stream().map(this::toResponse).toList();
        }
        Set<UUID> seen = new LinkedHashSet<>();
        List<CenterResponse> result = new ArrayList<>();
        // Centers owned by this user
        for (CoachingCenter c : centerRepository.findByOwnerId(principal.userId())) {
            if (seen.add(c.getId())) result.add(toResponse(c));
        }
        // Centers where this user is an assigned teacher
        for (var t : teacherRepository.findByUserId(principal.userId())) {
            centerRepository.findById(t.getCenterId()).ifPresent(c -> {
                if (seen.add(c.getId())) result.add(toResponse(c));
            });
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CenterResponse getCenter(UUID centerId, AuthPrincipal principal) {
        CoachingCenter center = centerRepository.findById(centerId)
            .orElseThrow(() -> new CenterNotFoundException(centerId));
        if (!principal.belongsToCenter(centerId, center.getAdminUserId())) {
            throw new CenterAccessDeniedException();
        }
        return toResponse(center);
    }

    @Transactional(readOnly = true)
    public Optional<CenterLookupResponse> lookupByCode(String code) {
        return centerRepository.findByCode(code)
            .map(c -> new CenterLookupResponse(c.getId(), c.getName(), c.getCity()));
    }

    private CenterResponse toResponse(CoachingCenter c) {
        return new CenterResponse(c.getId(), c.getName(), c.getCode(), c.getAddress(),
                c.getCity(), c.getState(), c.getPincode(), c.getPhone(), c.getEmail(),
                c.getWebsite(), c.getLogoUrl(), c.getStatus(), c.getOwnerId(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
