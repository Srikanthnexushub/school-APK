package com.edutech.student.application.dto;

import com.edutech.student.domain.model.Board;
import com.edutech.student.domain.model.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateStudentProfileRequest(
        UUID userId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email String email,
        String phone,
        Gender gender,
        @NotNull LocalDate dateOfBirth,
        String city,
        String state,
        String pincode,
        Board board,
        @Min(10) @Max(13) Integer currentClass,
        List<String> subjects,
        String institutionName
) {}
