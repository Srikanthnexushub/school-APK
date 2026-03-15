// src/main/java/com/edutech/center/application/service/TeacherService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AssignTeacherRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.TeacherResponse;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.application.exception.TeacherAlreadyAssignedException;
import com.edutech.center.domain.event.TeacherAssignedEvent;
import com.edutech.center.domain.model.Teacher;
import com.edutech.center.domain.port.in.AssignTeacherUseCase;
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

import java.util.List;
import java.util.UUID;

@Service
public class TeacherService implements AssignTeacherUseCase {

    private static final Logger log = LoggerFactory.getLogger(TeacherService.class);

    private final TeacherRepository teacherRepository;
    private final CenterRepository centerRepository;
    private final CenterEventPublisher eventPublisher;

    public TeacherService(TeacherRepository teacherRepository,
                          CenterRepository centerRepository,
                          CenterEventPublisher eventPublisher) {
        this.teacherRepository = teacherRepository;
        this.centerRepository = centerRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Returns true if the principal may act on the given centerId.
     * Fetches the center once so that the adminUserId comparison is available,
     * covering CENTER_ADMINs whose JWT still has centerId=null (Kafka not yet synced).
     */
    private boolean hasAccessToCenter(AuthPrincipal principal, UUID centerId) {
        if (principal.isSuperAdmin()) return true;
        if (principal.belongsToCenter(centerId)) return true;
        return centerRepository.findById(centerId)
                .map(c -> principal.belongsToCenter(centerId, c.getAdminUserId()))
                .orElse(false);
    }

    @Override
    @Transactional
    public TeacherResponse assignTeacher(UUID centerId, AssignTeacherRequest request, AuthPrincipal principal) {
        if (!hasAccessToCenter(principal, centerId)) {
            throw new CenterAccessDeniedException();
        }
        centerRepository.findById(centerId)
            .orElseThrow(() -> new CenterNotFoundException(centerId));

        if (teacherRepository.existsByUserIdAndCenterId(request.userId(), centerId)) {
            throw new TeacherAlreadyAssignedException(request.userId(), centerId);
        }

        Teacher teacher = Teacher.create(centerId, request.userId(), request.firstName(),
                request.lastName(), request.email(), request.phoneNumber(), request.subjects());

        Teacher saved = teacherRepository.save(teacher);

        eventPublisher.publish(new TeacherAssignedEvent(
                saved.getId(), centerId, saved.getUserId(), saved.getEmail()));

        log.info("Teacher assigned: teacherId={} centerId={}", saved.getId(), centerId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> listTeachers(UUID centerId, AuthPrincipal principal) {
        if (!hasAccessToCenter(principal, centerId)) {
            throw new CenterAccessDeniedException();
        }
        return teacherRepository.findByCenterId(centerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<TeacherResponse> listTeachers(UUID centerId, AuthPrincipal principal, Pageable pageable) {
        if (!hasAccessToCenter(principal, centerId)) {
            throw new CenterAccessDeniedException();
        }
        List<TeacherResponse> all = teacherRepository.findByCenterId(centerId).stream()
                .map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    private TeacherResponse toResponse(Teacher t) {
        return new TeacherResponse(t.getId(), t.getCenterId(), t.getUserId(),
                t.getFirstName(), t.getLastName(), t.getEmail(),
                t.getPhoneNumber(), t.getSubjects(), t.getDistrict(), t.getEmployeeId(),
                t.getStatus(), t.getJoinedAt());
    }
}
