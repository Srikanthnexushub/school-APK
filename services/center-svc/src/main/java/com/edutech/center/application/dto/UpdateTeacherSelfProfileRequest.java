package com.edutech.center.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateTeacherSelfProfileRequest(
    @Size(max = 20)   String phoneNumber,
    @Size(max = 500)  String subjects,
    @Size(max = 100)  String district,
    @Size(max = 200)  String designation,
    @Size(max = 200)  String qualification,
                      Integer yearsOfExperience,
    @Size(max = 2000) String bio
) {}
