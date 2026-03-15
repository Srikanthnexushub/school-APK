package com.edutech.student.application.dto;

import com.edutech.student.domain.model.Gender;
import com.edutech.student.domain.model.Stream;

public record UpdateStudentProfileRequest(
        String firstName,
        String lastName,
        String phone,
        Gender gender,
        String city,
        String state,
        Stream stream,
        Integer targetYear
) {}
