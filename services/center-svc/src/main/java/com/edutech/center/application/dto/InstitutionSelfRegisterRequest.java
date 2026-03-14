package com.edutech.center.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitutionSelfRegisterRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 20) String phone,
    @Size(max = 500) String address,
    @Size(max = 100) String state,
    @Size(max = 10) String pincode
) {}
