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

    @Override
    @Transactional
    public TeacherResponse assignTeacher(UUID centerId, AssignTeacherRequest request, AuthPrincipal principal) {
        if (!principal.belongsToCenter(centerId)) {
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
        if (!principal.belongsToCenter(centerId)) {
            throw new CenterAccessDeniedException();
        }
        return teacherRepository.findByCenterId(centerId).stream().map(this::toResponse).toList();
    }

    private TeacherResponse toResponse(Teacher t) {
        return new TeacherResponse(t.getId(), t.getCenterId(), t.getUserId(),
                t.getFirstName(), t.getLastName(), t.getEmail(),
                t.getPhoneNumber(), t.getSubjects(), t.getStatus(), t.getJoinedAt());
    }
}
