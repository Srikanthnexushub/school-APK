// src/main/java/com/edutech/center/api/InvitationController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.InvitationLookupResponse;
import com.edutech.center.application.service.TeacherBulkImportService;
import com.edutech.center.domain.model.Teacher;
import com.edutech.center.domain.port.out.CenterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers/invitation")
@Tag(name = "Invitation", description = "Public invitation token resolution")
public class InvitationController {

    private final TeacherBulkImportService bulkImportService;
    private final CenterRepository centerRepository;

    public InvitationController(TeacherBulkImportService bulkImportService,
                                 CenterRepository centerRepository) {
        this.bulkImportService = bulkImportService;
        this.centerRepository = centerRepository;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Resolve an invitation token — public, no auth required")
    public ResponseEntity<InvitationLookupResponse> lookupToken(@PathVariable String token) {
        return bulkImportService.findByToken(token)
            .map(teacher -> {
                String centerName = centerRepository.findById(teacher.getCenterId())
                    .map(c -> c.getName()).orElse("Institution");
                return ResponseEntity.ok(new InvitationLookupResponse(
                    teacher.getId(), teacher.getCenterId(), centerName,
                    teacher.getEmail(), teacher.getFirstName(), teacher.getLastName()
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
