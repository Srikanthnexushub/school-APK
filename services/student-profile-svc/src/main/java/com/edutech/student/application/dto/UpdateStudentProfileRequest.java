package com.edutech.student.application.dto;

import com.edutech.student.domain.model.Stream;

public record UpdateStudentProfileRequest(
        String firstName,
        String lastName,
        String phone,
        String city,
        String state,
        Stream stream,
        Integer targetYear
) {}
