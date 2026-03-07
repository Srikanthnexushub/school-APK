// src/main/java/com/edutech/center/application/service/FeeService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CreateFeeStructureRequest;
import com.edutech.center.application.dto.FeeStructureResponse;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.domain.model.FeeStructure;
import com.edutech.center.domain.port.in.CreateFeeStructureUseCase;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.FeeStructureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FeeService implements CreateFeeStructureUseCase {

    private static final Logger log = LoggerFactory.getLogger(FeeService.class);

    private final FeeStructureRepository feeStructureRepository;
    private final CenterRepository centerRepository;

    public FeeService(FeeStructureRepository feeStructureRepository,
                      CenterRepository centerRepository) {
        this.feeStructureRepository = feeStructureRepository;
        this.centerRepository = centerRepository;
    }

    @Override
    @Transactional
    public FeeStructureResponse createFeeStructure(UUID centerId, CreateFeeStructureRequest request,
                                                   AuthPrincipal principal) {
        if (!principal.belongsToCenter(centerId)) {
            throw new CenterAccessDeniedException();
        }
        centerRepository.findById(centerId)
            .orElseThrow(() -> new CenterNotFoundException(centerId));

        FeeStructure fee = FeeStructure.create(centerId, request.name(), request.description(),
                request.amount(), request.currency(), request.frequency(),
                request.dueDay(), request.lateFeeAmount());

        FeeStructure saved = feeStructureRepository.save(fee);
        log.info("Fee structure created: id={} centerId={}", saved.getId(), centerId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> listFeeStructures(UUID centerId, AuthPrincipal principal) {
        if (!principal.belongsToCenter(centerId)) {
            throw new CenterAccessDeniedException();
        }
        return feeStructureRepository.findByCenterId(centerId).stream().map(this::toResponse).toList();
    }

    private FeeStructureResponse toResponse(FeeStructure f) {
        return new FeeStructureResponse(f.getId(), f.getCenterId(), f.getName(),
                f.getDescription(), f.getAmount(), f.getCurrency(), f.getFrequency(),
                f.getDueDay(), f.getLateFeeAmount(), f.getStatus(), f.getCreatedAt());
    }
}
