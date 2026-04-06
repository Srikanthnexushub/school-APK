package com.edutech.student.application.dto;

import com.edutech.student.domain.model.Board;
import com.edutech.student.domain.model.CollegeProgram;
import com.edutech.student.domain.model.Gender;
import com.edutech.student.domain.model.ProfileStatus;
import com.edutech.student.domain.model.Stream;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentProfileResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        Gender gender,
        LocalDate dateOfBirth,
        String city,
        String state,
        String district,
        String pincode,
        String country,
        Board board,
        Integer currentClass,
        Stream stream,
        CollegeProgram collegeProgram,
        Integer targetYear,
        ProfileStatus status,
        Instant createdAt,
        List<String> subjects,
        String parentLinkCode
) {}
