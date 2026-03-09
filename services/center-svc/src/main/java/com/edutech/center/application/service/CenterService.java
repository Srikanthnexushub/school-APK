// src/main/java/com/edutech/center/application/service/CenterService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateCenterRequest;
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
public class CenterService implements CreateCenterUseCase, UpdateCenterUseCase {

    private static final Logger log = LoggerFactory.getLogger(CenterService.class);

    private final CenterRepository centerRepository;
    private final CenterEventPublisher eventPublisher;

    public CenterService(CenterRepository centerRepository,
                         CenterEventPublisher eventPublisher) {
        this.centerRepository = centerRepository;
        this.eventPublisher = eventPublisher;
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

    @Transactional(readOnly = true)
    public List<CenterResponse> listCenters(AuthPrincipal principal) {
        List<CoachingCenter> centers = principal.isSuperAdmin()
            ? centerRepository.findAll()
            : centerRepository.findByOwnerId(principal.userId());
        return centers.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<CenterResponse> listCenters(AuthPrincipal principal, Pageable pageable) {
        List<CenterResponse> all = principal.isSuperAdmin()
            ? centerRepository.findAll().stream().map(this::toResponse).toList()
            : centerRepository.findByOwnerId(principal.userId()).stream().map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    @Transactional(readOnly = true)
    public CenterResponse getCenter(UUID centerId, AuthPrincipal principal) {
        CoachingCenter center = centerRepository.findById(centerId)
            .orElseThrow(() -> new CenterNotFoundException(centerId));
        if (!principal.belongsToCenter(centerId)) {
            throw new CenterAccessDeniedException();
        }
        return toResponse(center);
    }

    private CenterResponse toResponse(CoachingCenter c) {
        return new CenterResponse(c.getId(), c.getName(), c.getCode(), c.getAddress(),
                c.getCity(), c.getState(), c.getPincode(), c.getPhone(), c.getEmail(),
                c.getWebsite(), c.getLogoUrl(), c.getStatus(), c.getOwnerId(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
