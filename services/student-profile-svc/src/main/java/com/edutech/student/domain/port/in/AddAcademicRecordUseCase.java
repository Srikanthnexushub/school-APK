package com.edutech.student.domain.port.in;

import com.edutech.student.application.dto.AcademicRecordResponse;
import com.edutech.student.application.dto.AddAcademicRecordRequest;

import java.util.UUID;

public interface AddAcademicRecordUseCase {
    AcademicRecordResponse addRecord(UUID studentId, AddAcademicRecordRequest request);
}
