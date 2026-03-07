package com.edutech.student.api;

import com.edutech.student.application.dto.AcademicRecordResponse;
import com.edutech.student.application.dto.AddAcademicRecordRequest;
import com.edutech.student.application.service.AcademicRecordService;
import com.edutech.student.domain.port.in.AddAcademicRecordUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students/{studentId}/academic-records")
public class AcademicRecordController {

    private final AddAcademicRecordUseCase addAcademicRecordUseCase;
    private final AcademicRecordService academicRecordService;

    public AcademicRecordController(AddAcademicRecordUseCase addAcademicRecordUseCase,
                                     AcademicRecordService academicRecordService) {
        this.addAcademicRecordUseCase = addAcademicRecordUseCase;
        this.academicRecordService = academicRecordService;
    }

    @PostMapping
    public ResponseEntity<AcademicRecordResponse> addRecord(
            @PathVariable UUID studentId,
            @Valid @RequestBody AddAcademicRecordRequest request) {
        AcademicRecordResponse response = addAcademicRecordUseCase.addRecord(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AcademicRecordResponse>> getRecords(@PathVariable UUID studentId) {
        return ResponseEntity.ok(academicRecordService.getRecordsByStudentId(studentId));
    }
}
