package com.edutech.student.application.dto;

import java.util.UUID;

public record StudentLookupResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String city,
        String board,
        Integer currentClass
) {}
